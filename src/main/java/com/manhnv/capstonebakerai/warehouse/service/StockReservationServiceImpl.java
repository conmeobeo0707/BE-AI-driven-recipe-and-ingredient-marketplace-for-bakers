package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.dtos.ComboItemRequest;
import com.manhnv.capstonebakerai.product.dtos.InventoryProduct;
import com.manhnv.capstonebakerai.product.dtos.InventoryUnit;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.warehouse.dto.*;
import com.manhnv.capstonebakerai.warehouse.entity.*;
import com.manhnv.capstonebakerai.warehouse.enums.*;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StockReservationServiceImpl implements StockReservationService {
    private final ProductService products;
    private final StockLotRepository lots;
    private final StockReservationRepository reservations;
    private final StockReservationItemRepository reservationItems;
    private final StockOutNoteRepository stockOutNotes;
    private final StockOutItemRepository stockOutItems;

    public StockReservationServiceImpl(ProductService products, StockLotRepository lots,
                                       StockReservationRepository reservations,
                                       StockReservationItemRepository reservationItems,
                                       StockOutNoteRepository stockOutNotes,
                                       StockOutItemRepository stockOutItems) {
        this.products = products;
        this.lots = lots;
        this.reservations = reservations;
        this.reservationItems = reservationItems;
        this.stockOutNotes = stockOutNotes;
        this.stockOutItems = stockOutItems;
    }

    @Override
    @Transactional
    public StockReservationResponse reserve(StockReservationCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        reservations.expirePastDue(ReservationStatus.ACTIVE, ReservationStatus.EXPIRED, now);

        Map<UUID, BigDecimal> requestedBaseQuantities = new TreeMap<>();
        Map<UUID, UUID> baseUnitIdsByProduct = new HashMap<>();
        Set<UUID> productsToLock = new TreeSet<>();

        for (StockReservationCreateRequest.Item requestedItem : request.items()) {
            InventoryProduct requestedProduct = products.getInventoryProduct(requestedItem.productId());
            InventoryUnit requestedUnit = products.getActiveInventoryUnit(requestedItem.productId(), requestedItem.unitId());
            BigDecimal baseQuantity = requestedItem.quantity().multiply(requestedUnit.conversionFactor())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            if (baseQuantity.signum() <= 0 || baseQuantity.precision() - baseQuantity.scale() > 8) {
                throw WarehouseSupport.error("Converted reservation quantity exceeds the database precision limit");
            }
            expandIntoStockedProducts(requestedProduct.productId(), baseQuantity, new HashSet<>(),
                    requestedBaseQuantities, baseUnitIdsByProduct, productsToLock);
        }
        products.lockInventoryProducts(productsToLock);
        List<UUID> productIds = requestedBaseQuantities.keySet().stream().sorted().toList();

        Optional<StockReservation> existingReservation = reservations.lockByOrderId(request.orderId());
        if (existingReservation.isPresent()) {
            StockReservation reservation = existingReservation.get();
            if (reservation.getStatus() == ReservationStatus.ACTIVE && reservation.getExpiresAt().isAfter(now)
                    && matchesReservation(reservation, requestedBaseQuantities)) {
                return toResponse(reservation, reservationItems.findByReservationIdOrderByItemId(reservation.getReservationId()));
            }
            throw new AppException(WarehouseErrorCode.CONFLICT, "An inventory reservation already exists for this order");
        }

        List<StockLot> saleableLots = new ArrayList<>();
        for (UUID productId : productIds) {
            saleableLots.addAll(lots.findSaleableLots(productId, LotStatus.STORAGE, LocalDate.now()));
        }
        List<UUID> saleableLotIds = saleableLots.stream().map(StockLot::getLotId).distinct().toList();
        List<StockLot> lockedLots = saleableLotIds.isEmpty() ? List.of() : lots.lockByLotIds(saleableLotIds);
        Map<UUID, StockLot> lockedLotsById = lockedLots.stream().collect(Collectors.toMap(StockLot::getLotId, stockLot -> stockLot));
        List<StockReservationItem> newReservationItems = new ArrayList<>();

        for (UUID productId : productIds) {
            BigDecimal remainingQuantity = requestedBaseQuantities.get(productId);
            List<StockLot> productLots = saleableLots.stream()
                    .filter(stockLot -> stockLot.getProductId().equals(productId))
                    .map(stockLot -> lockedLotsById.get(stockLot.getLotId()))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(StockLot::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(StockLot::getCreatedAt).thenComparing(StockLot::getLotId))
                    .toList();
            for (StockLot stockLot : productLots) {
                BigDecimal alreadyReserved = reservationItems.sumActiveQuantityByLotId(stockLot.getLotId(), ReservationStatus.ACTIVE, now);
                BigDecimal availableOnLot = stockLot.getCurrentQuantity().subtract(alreadyReserved).max(BigDecimal.ZERO);
                BigDecimal allocatedQuantity = remainingQuantity.min(availableOnLot);
                if (allocatedQuantity.signum() <= 0) continue;
                StockReservationItem reservationItem = new StockReservationItem();
                reservationItem.setProductId(productId);
                reservationItem.setLotId(stockLot.getLotId());
                reservationItem.setUnitId(baseUnitIdsByProduct.get(productId));
                reservationItem.setQuantity(allocatedQuantity);
                newReservationItems.add(reservationItem);
                remainingQuantity = remainingQuantity.subtract(allocatedQuantity);
                if (remainingQuantity.signum() == 0) break;
            }
            if (remainingQuantity.signum() > 0) {
                throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK, "Insufficient available stock for product " + productId);
            }
        }

        StockReservation reservation = new StockReservation();
        reservation.setOrderId(request.orderId());
        reservation.setExpiresAt(request.expiresAt());
        StockReservation savedReservation = reservations.save(reservation);
        for (StockReservationItem reservationItem : newReservationItems) {
            reservationItem.setReservationId(savedReservation.getReservationId());
        }
        List<StockReservationItem> savedItems = reservationItems.saveAll(newReservationItems);
        return toResponse(savedReservation, savedItems);
    }

    @Override
    @Transactional(readOnly = true)
    public StockReservationResponse getByOrderId(UUID orderId) {
        StockReservation reservation = reservations.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Reservation was not found for order " + orderId));
        if (reservation.getStatus() == ReservationStatus.ACTIVE && !reservation.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new AppException(WarehouseErrorCode.CONFLICT, "Reservation has expired");
        }
        return toResponse(reservation, reservationItems.findByReservationIdOrderByItemId(reservation.getReservationId()));
    }

    @Override
    @Transactional
    public StockReservationResponse release(UUID orderId) {
        StockReservation reservation = reservations.lockByOrderId(orderId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Reservation was not found for order " + orderId));
        if (reservation.getStatus() == ReservationStatus.ACTIVE) {
            reservation.setStatus(reservation.getExpiresAt().isAfter(LocalDateTime.now()) ? ReservationStatus.RELEASED : ReservationStatus.EXPIRED);
            reservation.setUpdatedAt(LocalDateTime.now());
            reservations.save(reservation);
        }
        return toResponse(reservation, reservationItems.findByReservationIdOrderByItemId(reservation.getReservationId()));
    }

    @Override
    @Transactional
    public StockOutNote consume(UUID orderId) {
        StockReservation observedReservation = reservations.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Reservation was not found for order " + orderId));
        if (observedReservation.getStatus() == ReservationStatus.CONSUMED) {
            return stockOutNotes.findByReferenceOrderIdAndType(orderId, StockOutType.SALE)
                    .orElseThrow(() -> new AppException(WarehouseErrorCode.CONFLICT, "Consumed reservation has no stock-out note"));
        }
        List<StockReservationItem> observedItems = reservationItems.findByReservationIdOrderByItemId(observedReservation.getReservationId());
        List<UUID> observedProductIds = observedItems.stream().map(StockReservationItem::getProductId).distinct().sorted().toList();
        products.lockInventoryProducts(observedProductIds);
        StockReservation reservation = reservations.lockByOrderId(orderId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Reservation was not found for order " + orderId));
        if (reservation.getStatus() == ReservationStatus.CONSUMED) {
            return stockOutNotes.findByReferenceOrderIdAndType(orderId, StockOutType.SALE)
                    .orElseThrow(() -> new AppException(WarehouseErrorCode.CONFLICT, "Consumed reservation has no stock-out note"));
        }
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new AppException(WarehouseErrorCode.CONFLICT, "Only active reservations can be consumed");
        }
        if (!reservation.getExpiresAt().isAfter(LocalDateTime.now())) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setUpdatedAt(LocalDateTime.now());
            reservations.save(reservation);
            throw new AppException(WarehouseErrorCode.CONFLICT, "Reservation has expired");
        }

        List<StockReservationItem> reservedItems = reservationItems.findByReservationIdOrderByItemId(reservation.getReservationId());
        List<UUID> lotIds = reservedItems.stream().map(StockReservationItem::getLotId).distinct().sorted().toList();
        Map<UUID, StockLot> stockLotsById = lots.lockByLotIds(lotIds).stream()
                .collect(Collectors.toMap(StockLot::getLotId, stockLot -> stockLot));

        StockOutNote stockOutNote = new StockOutNote();
        stockOutNote.setCode(WarehouseSupport.code("SOUT"));
        stockOutNote.setType(StockOutType.SALE);
        stockOutNote.setReferenceOrderId(orderId);
        stockOutNote.setNote("Sale order " + orderId + "; consumed inventory reservation " + reservation.getReservationId());
        stockOutNote.setStatus(NoteStatus.COMPLETED);
        StockOutNote savedStockOutNote = stockOutNotes.save(stockOutNote);
        BigDecimal totalCost = BigDecimal.ZERO;
        Map<UUID, BigDecimal> stockDeltas = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (StockReservationItem reservationItem : reservedItems) {
            StockLot stockLot = stockLotsById.get(reservationItem.getLotId());
            if (stockLot == null || !stockLot.getProductId().equals(reservationItem.getProductId())
                    || stockLot.getCurrentQuantity().compareTo(reservationItem.getQuantity()) < 0
                    || stockLot.getStatus() != LotStatus.STORAGE
                    || stockLot.getExpiryDate() != null && stockLot.getExpiryDate().isBefore(today)) {
                throw new AppException(WarehouseErrorCode.CONFLICT, "Reserved lot is no longer available; order requires review");
            }
            BigDecimal lineCost = WarehouseSupport.money(reservationItem.getQuantity().multiply(stockLot.getUnitCost()));
            if (lineCost.precision() - lineCost.scale() > 13) throw WarehouseSupport.error("Stock-out line cost exceeds the database precision limit");
            StockOutItem stockOutItem = new StockOutItem();
            stockOutItem.setNoteId(savedStockOutNote.getNoteId());
            stockOutItem.setProductId(reservationItem.getProductId());
            stockOutItem.setLotId(stockLot.getLotId());
            stockOutItem.setUnitId(reservationItem.getUnitId());
            stockOutItem.setQuantity(reservationItem.getQuantity());
            stockOutItem.setUnitPrice(stockLot.getUnitCost());
            stockOutItem.setTotalPrice(lineCost);
            totalCost = totalCost.add(lineCost);

            stockLot.setCurrentQuantity(stockLot.getCurrentQuantity().subtract(reservationItem.getQuantity()));
            stockLot.setStatus(stockLot.getCurrentQuantity().signum() == 0 ? LotStatus.EMPTY : LotStatus.STORAGE);
            stockDeltas.merge(reservationItem.getProductId(), reservationItem.getQuantity().negate(), BigDecimal::add);
            stockOutItems.save(stockOutItem);
        }
        totalCost = WarehouseSupport.money(totalCost);
        if (totalCost.precision() - totalCost.scale() > 13) throw WarehouseSupport.error("Stock-out total cost exceeds the database precision limit");
        savedStockOutNote.setTotalCost(totalCost);
        savedStockOutNote.setUpdatedAt(LocalDateTime.now());
        stockOutNotes.save(savedStockOutNote);
        products.adjustInventoryStock(stockDeltas);
        reservation.setStatus(ReservationStatus.CONSUMED);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservations.save(reservation);
        return savedStockOutNote;
    }

    @Override
    @Transactional
    public AvailableInventoryResponse getAvailableInventory(UUID productId) {
        reservations.expirePastDue(ReservationStatus.ACTIVE, ReservationStatus.EXPIRED, LocalDateTime.now());
        StockQuantities quantities = calculateAvailableQuantities(productId, new HashSet<>());
        return new AvailableInventoryResponse(productId, quantities.saleableOnHand(), quantities.reserved(), quantities.available());
    }

    @Override
    @Transactional
    public int expirePastDueReservations() {
        return reservations.expirePastDue(ReservationStatus.ACTIVE, ReservationStatus.EXPIRED, LocalDateTime.now());
    }

    private boolean matchesReservation(StockReservation reservation, Map<UUID, BigDecimal> requestedBaseQuantities) {
        Map<UUID, BigDecimal> reservedByProduct = reservationItems.findByReservationIdOrderByItemId(reservation.getReservationId()).stream()
                .collect(Collectors.groupingBy(StockReservationItem::getProductId,
                        Collectors.reducing(BigDecimal.ZERO, StockReservationItem::getQuantity, BigDecimal::add)));
        return reservedByProduct.equals(requestedBaseQuantities);
    }

    private StockQuantities calculateAvailableQuantities(UUID productId, Set<UUID> productsInPath) {
        if (!productsInPath.add(productId)) throw WarehouseSupport.error("Combo composition contains a cycle");
        InventoryProduct inventoryProduct = products.getInventoryProduct(productId);
        if (!inventoryProduct.combo()) {
            List<StockLot> saleableLots = lots.findSaleableLots(productId, LotStatus.STORAGE, LocalDate.now());
            BigDecimal saleableOnHand = saleableLots.stream().map(StockLot::getCurrentQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal reserved = saleableLots.stream()
                    .map(stockLot -> reservationItems.sumActiveQuantityByLotId(stockLot.getLotId(), ReservationStatus.ACTIVE, LocalDateTime.now()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            productsInPath.remove(productId);
            return new StockQuantities(saleableOnHand, reserved, saleableOnHand.subtract(reserved).max(BigDecimal.ZERO));
        }

        List<ComboItemRequest> comboItems = products.getComboItems(productId);
        if (comboItems.isEmpty()) {
            productsInPath.remove(productId);
            return new StockQuantities(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal comboOnHand = null;
        BigDecimal comboAvailable = null;
        for (ComboItemRequest comboItem : comboItems) {
            StockQuantities componentQuantities = calculateAvailableQuantities(comboItem.productId(), productsInPath);
            BigDecimal possibleOnHand = componentQuantities.saleableOnHand().divide(comboItem.quantity(), 6, java.math.RoundingMode.DOWN);
            BigDecimal possibleAvailable = componentQuantities.available().divide(comboItem.quantity(), 6, java.math.RoundingMode.DOWN);
            comboOnHand = comboOnHand == null || possibleOnHand.compareTo(comboOnHand) < 0 ? possibleOnHand : comboOnHand;
            comboAvailable = comboAvailable == null || possibleAvailable.compareTo(comboAvailable) < 0 ? possibleAvailable : comboAvailable;
        }
        productsInPath.remove(productId);
        BigDecimal onHand = comboOnHand == null ? BigDecimal.ZERO : comboOnHand;
        BigDecimal available = comboAvailable == null ? BigDecimal.ZERO : comboAvailable;
        return new StockQuantities(onHand, onHand.subtract(available).max(BigDecimal.ZERO), available);
    }

    private void expandIntoStockedProducts(UUID productId, BigDecimal quantity, Set<UUID> productsInPath,
                                           Map<UUID, BigDecimal> baseQuantitiesByProduct,
                                           Map<UUID, UUID> baseUnitIdsByProduct,
                                           Set<UUID> productsToLock) {
        if (!productsInPath.add(productId)) throw WarehouseSupport.error("Combo composition contains a cycle");
        productsToLock.add(productId);
        InventoryProduct inventoryProduct = products.getInventoryProduct(productId);
        if (inventoryProduct.combo()) {
            List<ComboItemRequest> comboItems = products.getComboItems(productId);
            if (comboItems.isEmpty()) throw WarehouseSupport.error("Combo has no components: " + productId);
            for (ComboItemRequest comboItem : comboItems) {
                BigDecimal componentQuantity = quantity.multiply(comboItem.quantity()).setScale(2, java.math.RoundingMode.HALF_UP);
                expandIntoStockedProducts(comboItem.productId(), componentQuantity, productsInPath,
                        baseQuantitiesByProduct, baseUnitIdsByProduct, productsToLock);
            }
        } else {
            InventoryUnit baseUnit = products.getActiveBaseInventoryUnit(productId);
            baseQuantitiesByProduct.merge(productId, quantity, BigDecimal::add);
            baseUnitIdsByProduct.put(productId, baseUnit.unitId());
        }
        productsInPath.remove(productId);
    }

    private record StockQuantities(BigDecimal saleableOnHand, BigDecimal reserved, BigDecimal available) { }

    private StockReservationResponse toResponse(StockReservation reservation, List<StockReservationItem> items) {
        List<StockReservationResponse.Item> responseItems = items.stream()
                .map(item -> new StockReservationResponse.Item(item.getProductId(), item.getLotId(), item.getUnitId(), item.getQuantity()))
                .toList();
        return new StockReservationResponse(reservation.getReservationId(), reservation.getOrderId(), reservation.getStatus(), reservation.getExpiresAt(), responseItems);
    }
}
