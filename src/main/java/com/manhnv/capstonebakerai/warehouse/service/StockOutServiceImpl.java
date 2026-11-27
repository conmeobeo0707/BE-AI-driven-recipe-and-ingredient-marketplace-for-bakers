package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.product.dtos.InventoryProduct;
import com.manhnv.capstonebakerai.product.dtos.InventoryUnit;
import com.manhnv.capstonebakerai.product.service.ProductService;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.warehouse.dto.*;
import com.manhnv.capstonebakerai.warehouse.entity.*;
import com.manhnv.capstonebakerai.warehouse.enums.*;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class StockOutServiceImpl implements StockOutService {
    private final StockOutNoteRepository notes;
    private final StockOutItemRepository items;
    private final StockLotRepository lots;
    private final WarehouseSupport support;
    private final ProductService products;
    private final StockTransactionService stockTransactions;
    private final StocktakeNoteRepository stocktakeNotes;
    private final StocktakeItemRepository stocktakeItems;
    private final UserRepository users;
    private final StockReservationRepository reservations;
    private final StockReservationItemRepository reservationItems;

    public StockOutServiceImpl(StockOutNoteRepository notes, StockOutItemRepository items, StockLotRepository lots,
                               WarehouseSupport support, ProductService products,
                               StockTransactionService stockTransactions, StocktakeNoteRepository stocktakeNotes,
                               StocktakeItemRepository stocktakeItems, UserRepository users,
                               StockReservationRepository reservations, StockReservationItemRepository reservationItems) {
        this.notes = notes; this.items = items; this.lots = lots; this.support = support; this.products = products;
        this.stockTransactions = stockTransactions;
        this.stocktakeNotes = stocktakeNotes;
        this.stocktakeItems = stocktakeItems;
        this.users = users;
        this.reservations = reservations;
        this.reservationItems = reservationItems;
    }

    @Override @Transactional(readOnly = true)
    public Page<StockOutNote> list(NoteStatus status, StockOutType type, Pageable pageable) {
        WarehouseSupport.requireWarehouseUser();
        if (status != null && type != null) return notes.findByStatusAndType(status, type, pageable);
        if (status != null) return notes.findByStatus(status, pageable);
        if (type != null) return notes.findByType(type, pageable);
        return notes.findAll(pageable);
    }

    @Override @Transactional(readOnly = true)
    public StockOutDetailResponse get(UUID noteId) {
        WarehouseSupport.requireWarehouseUser();
        StockOutNote stockOutNote = notes.findById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Stock-out note not found"));
        return new StockOutDetailResponse(stockOutNote, items.findByNoteIdOrderByItemId(noteId),
                userName(stockOutNote.getCreatedBy()), userName(stockOutNote.getApprovedBy()));
    }

    private String userName(UUID userId) {
        return userId == null ? null : users.findById(userId).map(User::getFullName).orElse(null);
    }

    @Override @Transactional
    public StockOutNote create(StockOutCreateRequest request) {
        WarehouseSupport.requireWarehouseUser();
        if (request.type() == StockOutType.DISPOSAL) WarehouseSupport.requireRole("STAFF");
        return createPending(request, SecurityUtils.getCurrentUserId(), null, Map.of());
    }

    private StockOutNote createPending(StockOutCreateRequest request, UUID creator, UUID stocktakeNoteId,
                                       Map<UUID, UUID> stocktakeItemsByLot) {
        return createPending(request, creator, stocktakeNoteId, stocktakeItemsByLot, false);
    }

    private StockOutNote createPending(StockOutCreateRequest request, UUID creator, UUID stocktakeNoteId,
                                       Map<UUID, UUID> stocktakeItemsByLot, boolean useReservedLots) {
        List<UUID> productIds = request.items().stream().map(StockOutItemRequest::productId).distinct().toList();
        Map<UUID, InventoryProduct> productsById = new HashMap<>();
        support.lockProducts(productIds).forEach(inventoryProduct -> productsById.put(inventoryProduct.productId(), inventoryProduct));
        StockOutNote stockOutNote = new StockOutNote();
        stockOutNote.setCode(WarehouseSupport.code("SOUT"));
        stockOutNote.setType(request.type());
        stockOutNote.setCreatedBy(creator);
        stockOutNote.setStocktakeNoteId(stocktakeNoteId);
        stockOutNote.setReferenceOrderId(request.referenceOrderId());
        stockOutNote.setNote(request.note());
        stockOutNote.setStatus(NoteStatus.PENDING);
        BigDecimal estimatedCost = BigDecimal.ZERO;
        List<StockOutItem> pendingItems = new ArrayList<>();
        for (StockOutItemRequest itemRequest : request.items()) {
            InventoryProduct inventoryProduct = productsById.get(itemRequest.productId());
            if (inventoryProduct == null) throw new AppException(WarehouseErrorCode.NOT_FOUND, "Product not found");
            InventoryUnit requestedUnit = support.unit(itemRequest.productId(), itemRequest.unitId());
            if (request.type() == StockOutType.SALE && itemRequest.lotId() != null && !useReservedLots) throw WarehouseSupport.error("SALE items use FEFO and must not specify lotId");
            if (request.type() == StockOutType.DISPOSAL && itemRequest.lotId() == null) throw WarehouseSupport.error("DISPOSAL items require lotId");
            BigDecimal unitCost = BigDecimal.ZERO;
            if (itemRequest.lotId() != null) {
                StockLot requestedLot = lots.findById(itemRequest.lotId()).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Lot not found"));
                if (!requestedLot.getProductId().equals(inventoryProduct.productId())) throw WarehouseSupport.error("Lot does not belong to product");
                if (request.type() == StockOutType.DISPOSAL) unitCost = requestedLot.getUnitCost();
                // Every disposal creation path holds the product lock before checking pending notes.
                if (request.type() == StockOutType.DISPOSAL && requestedLot.getExpiryDate() != null
                        && requestedLot.getExpiryDate().isBefore(LocalDate.now())
                        && items.hasDisposalForLot(requestedLot.getLotId(), NoteStatus.PENDING, StockOutType.DISPOSAL)) {
                    throw new AppException(WarehouseErrorCode.CONFLICT, "This expired lot already has a disposal awaiting approval");
                }
            }
            InventoryUnit baseUnit = support.baseUnit(inventoryProduct.productId());
            StockOutItem pendingItem = new StockOutItem();
            pendingItem.setProductId(inventoryProduct.productId());
            pendingItem.setLotId(itemRequest.lotId());
            if (itemRequest.lotId() != null) pendingItem.setStocktakeItemId(stocktakeItemsByLot.get(itemRequest.lotId()));
            pendingItem.setUnitId(baseUnit.unitId());
            BigDecimal baseQuantity = (stocktakeNoteId == null && !useReservedLots
                    ? itemRequest.quantity().multiply(requestedUnit.conversionFactor()) : itemRequest.quantity()).setScale(2, java.math.RoundingMode.HALF_UP);
            if (baseQuantity.signum() <= 0 || baseQuantity.precision() - baseQuantity.scale() > 8) throw WarehouseSupport.error("Converted stock quantity exceeds the database precision limit");
            pendingItem.setQuantity(baseQuantity);
            BigDecimal lineCost = WarehouseSupport.money(baseQuantity.multiply(unitCost));
            if (lineCost.precision() - lineCost.scale() > 13) throw WarehouseSupport.error("Stock-out line cost exceeds the database precision limit");
            pendingItem.setUnitPrice(unitCost);
            pendingItem.setTotalPrice(lineCost);
            estimatedCost = estimatedCost.add(lineCost);
            pendingItems.add(pendingItem);
        }
        if (estimatedCost.precision() - estimatedCost.scale() > 13) throw WarehouseSupport.error("Stock-out total cost exceeds the database precision limit");
        stockOutNote.setTotalCost(estimatedCost);
        StockOutNote savedNote = notes.save(stockOutNote);
        pendingItems.forEach(item -> item.setNoteId(savedNote.getNoteId()));
        items.saveAll(pendingItems);
        return savedNote;
    }

    @Override
    @Transactional
    public StockOutNote processSaleOrder(UUID orderId, List<StockLine> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) throw WarehouseSupport.error("A sale order must contain items");

        StockReservation reservation = reservations.lockByOrderId(orderId).orElse(null);
        StockOutNote existingNote = notes.lockByReferenceOrderIdAndType(orderId, StockOutType.SALE).orElse(null);
        if (existingNote != null) {
            if (existingNote.getStatus() == NoteStatus.COMPLETED) return existingNote;
            if (existingNote.getStatus() == NoteStatus.PENDING) return postStockOut(existingNote, null);
            throw new AppException(WarehouseErrorCode.CONFLICT, "A cancelled sale stock-out already exists for this order");
        }

        List<StockOutItemRequest> saleItems = reservation == null
                ? expandOrderItems(orderItems)
                : consumeReservation(reservation);
        StockOutCreateRequest stockOutRequest = new StockOutCreateRequest(StockOutType.SALE, orderId,
                "Sale order " + orderId, saleItems);
        StockOutNote pendingNote = createPending(stockOutRequest, null, null, Map.of(), reservation != null);
        return postStockOut(pendingNote, null);
    }

    @Override
    @Transactional
    public void reserveForOrder(UUID orderId, List<StockLine> orderItems, LocalDateTime expiresAt) {
        if (orderItems == null || orderItems.isEmpty()) throw WarehouseSupport.error("A sale order must contain items");
        List<StockOutItemRequest> orderLines = expandOrderItems(orderItems);
        support.lockProducts(orderLines.stream().map(StockOutItemRequest::productId).toList()).forEach(this::requireSaleProduct);
        LocalDateTime now = LocalDateTime.now();
        if (expiresAt == null || !expiresAt.isAfter(now)) throw WarehouseSupport.error("Reservation expiry must be in the future");

        StockReservation existing = reservations.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            if (isActive(existing, now)) return;
            throw new AppException(WarehouseErrorCode.CONFLICT, "Order reservation is no longer active");
        }

        StockReservation reservation = new StockReservation();
        reservation.setOrderId(orderId);
        reservation.setExpiresAt(expiresAt);
        reservations.save(reservation);

        List<StockReservationItem> allocations = new ArrayList<>();
        for (StockOutItemRequest line : orderLines) {
            BigDecimal remaining = line.quantity();
            List<StockLot> candidates = lots.findSaleableLots(line.productId(), LotStatus.STORAGE, expiresAt.toLocalDate());
            for (StockLot lot : candidates) {
                if (support.isLotInDraftStocktake(lot.getLotId())) continue;
                BigDecimal quantity = remaining.min(unreservedQuantity(lot, now, null, null));
                if (quantity.signum() <= 0) continue;
                StockReservationItem allocation = new StockReservationItem();
                allocation.setReservation(reservation);
                allocation.setProductId(line.productId());
                allocation.setLotId(lot.getLotId());
                allocation.setUnitId(line.unitId());
                allocation.setQuantity(quantity);
                allocations.add(allocation);
                remaining = remaining.subtract(quantity);
                if (remaining.signum() == 0) break;
            }
            if (remaining.signum() > 0) {
                throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK, "Insufficient stock for product " + line.productId());
            }
        }
        reservationItems.saveAll(allocations);
    }

    @Override
    @Transactional
    public void releaseOrderReservation(UUID orderId) {
        reservations.lockByOrderId(orderId).ifPresent(reservation -> {
            if (reservation.getStatus() != StockReservationStatus.ACTIVE) return;
            LocalDateTime now = LocalDateTime.now();
            reservation.setStatus(isActive(reservation, now) ? StockReservationStatus.RELEASED : StockReservationStatus.EXPIRED);
            reservation.setUpdatedAt(now);
        });
    }

    private List<StockOutItemRequest> consumeReservation(StockReservation reservation) {
        List<StockReservationItem> allocations = reservationItems
                .findByReservationReservationIdOrderByProductIdAscLotIdAsc(reservation.getReservationId());
        support.lockProducts(allocations.stream().map(StockReservationItem::getProductId).distinct().toList());
        LocalDateTime now = LocalDateTime.now();
        if (!isActive(reservation, now) || allocations.isEmpty()) {
            throw new AppException(WarehouseErrorCode.CONFLICT, "Order reservation is no longer active");
        }
        reservation.setStatus(StockReservationStatus.CONSUMED);
        reservation.setUpdatedAt(now);
        return allocations.stream().map(item -> new StockOutItemRequest(item.getProductId(), item.getUnitId(),
                item.getLotId(), item.getQuantity())).toList();
    }

    private boolean isActive(StockReservation reservation, LocalDateTime now) {
        return reservation.getStatus() == StockReservationStatus.ACTIVE && reservation.getExpiresAt().isAfter(now);
    }

    private BigDecimal unreservedQuantity(StockLot lot, LocalDateTime now, UUID excludedNoteId, UUID excludedStocktakeId) {
        BigDecimal held = reservationItems.sumHeldQuantity(lot.getLotId(), StockReservationStatus.ACTIVE, now);
        UUID excluded = excludedStocktakeId == null ? new UUID(0, 0) : excludedStocktakeId;
        BigDecimal damaged = lots.sumStocktakeDamageHold(lot.getLotId(), excluded);
        return lot.getCurrentQuantity().subtract(held == null ? BigDecimal.ZERO : held)
                .subtract(approvedDisposalQuantity(lot.getLotId(), excludedNoteId))
                .subtract(damaged == null ? BigDecimal.ZERO : damaged);
    }

    private BigDecimal approvedDisposalQuantity(UUID lotId, UUID excludedNoteId) {
        BigDecimal held = items.sumApprovedDisposalQuantity(lotId, NoteStatus.PENDING, StockOutType.DISPOSAL, excludedNoteId);
        return held == null ? BigDecimal.ZERO : held;
    }

    private List<StockOutItemRequest> expandOrderItems(List<StockLine> orderItems) {
        Map<UUID, BigDecimal> baseQuantitiesByProduct = new TreeMap<>();
        for (StockLine orderItem : orderItems) {
            InventoryUnit selectedUnit = products.getActiveInventoryUnit(orderItem.productId(), orderItem.productUnitId());
            BigDecimal baseQuantity = BigDecimal.valueOf(orderItem.quantity()).multiply(selectedUnit.conversionFactor());
            if (baseQuantity.signum() <= 0 || baseQuantity.precision() - baseQuantity.scale() > 8) {
                throw WarehouseSupport.error("Converted stock quantity exceeds the database precision limit");
            }
            products.getSaleRequirements(orderItem.productId()).forEach((productId, perUnit) ->
                    baseQuantitiesByProduct.merge(productId, baseQuantity.multiply(perUnit), BigDecimal::add));
        }

        List<StockOutItemRequest> expanded = new ArrayList<>();
        for (var entry : baseQuantitiesByProduct.entrySet()) {
            UUID productId = entry.getKey();
            // Round the combined requirement once to the lot quantity precision.
            BigDecimal quantity = entry.getValue().setScale(2, RoundingMode.UP);
            if (quantity.precision() - quantity.scale() > 8) {
                throw WarehouseSupport.error("Converted stock quantity exceeds the database precision limit");
            }
            UUID baseUnitId = products.getActiveBaseInventoryUnit(productId).unitId();
            expanded.add(new StockOutItemRequest(productId, baseUnitId, null, quantity));
        }
        return expanded;
    }

    @Override
    @Transactional
    public void cancelSaleOrder(UUID orderId) {
        StockOutNote stockOutNote = notes.lockByReferenceOrderIdAndType(orderId, StockOutType.SALE).orElse(null);
        if (stockOutNote == null || stockOutNote.getStatus() == NoteStatus.CANCELLED) return;
        if (stockOutNote.getStatus() == NoteStatus.PENDING) {
            stockOutNote.setStatus(NoteStatus.CANCELLED);
            stockOutNote.setUpdatedAt(LocalDateTime.now());
            notes.save(stockOutNote);
            return;
        }
        if (stockOutNote.getStatus() != NoteStatus.COMPLETED) {
            throw new AppException(WarehouseErrorCode.CONFLICT, "Only a completed sale stock-out can be reversed");
        }

        List<StockOutItem> stockOutItems = items.findByNoteIdOrderByItemId(stockOutNote.getNoteId());
        List<UUID> lotIds = stockOutItems.stream().map(StockOutItem::getLotId).filter(Objects::nonNull).distinct().sorted().toList();
        List<UUID> productIds = stockOutItems.stream().map(StockOutItem::getProductId).distinct().sorted().toList();
        products.lockInventoryProducts(productIds);
        support.ensureLotsNotInDraftStocktake(lotIds);
        Map<UUID, StockLot> lotsById = lots.lockByLotIds(lotIds).stream()
                .collect(java.util.stream.Collectors.toMap(StockLot::getLotId, stockLot -> stockLot));

        Map<UUID, BigDecimal> stockDeltas = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (StockOutItem stockOutItem : stockOutItems) {
            StockLot stockLot = lotsById.get(stockOutItem.getLotId());
            if (stockLot == null || !stockLot.getProductId().equals(stockOutItem.getProductId())) {
                throw new AppException(WarehouseErrorCode.CONFLICT, "A stock-out lot is missing; inventory cannot be restored safely");
            }
            stockLot.setCurrentQuantity(stockLot.getCurrentQuantity().add(stockOutItem.getQuantity()));
            stockLot.setStatus(stockLot.getExpiryDate() != null && stockLot.getExpiryDate().isBefore(today)
                    ? LotStatus.EXPIRED : LotStatus.STORAGE);
            stockTransactions.record(stockLot.getLotId(), stockLot.getProductId(), StockTransactionType.SALE_REVERSAL,
                    stockOutItem.getQuantity(), stockLot.getCurrentQuantity(), StockTransactionRefType.STOCK_OUT_NOTE,
                    stockOutNote.getNoteId());
            stockDeltas.merge(stockOutItem.getProductId(), stockOutItem.getQuantity(), BigDecimal::add);
        }
        support.adjustStock(stockDeltas);
        stockOutNote.setStatus(NoteStatus.CANCELLED);
        stockOutNote.setUpdatedAt(LocalDateTime.now());
        notes.save(stockOutNote);
    }

    private void requireSaleProduct(InventoryProduct product) {
        if (product.status() == ProductStatus.DISCONTINUE) {
            throw new AppException(WarehouseErrorCode.CONFLICT, "Product is discontinued: " + product.productId());
        }
    }

    @Override @Transactional
    public StockOutNote createStocktakeDisposal(UUID stocktakeNoteId) {
        WarehouseSupport.requireRole("STAFF");
        UUID userId = SecurityUtils.getCurrentUserId();
        StocktakeNote stocktake = lockStocktake(stocktakeNoteId);
        if (!userId.equals(stocktake.getHandleBy())) throw new AppException(WarehouseErrorCode.FORBIDDEN);
        if (stocktake.getStatus() != StocktakeStatus.PENDING_DISPOSAL) throw new AppException(WarehouseErrorCode.CONFLICT);
        if (notes.findByStocktakeNoteIdAndStatus(stocktakeNoteId, NoteStatus.PENDING).isPresent()) {
            throw new AppException(WarehouseErrorCode.CONFLICT, "A disposal is already awaiting approval");
        }

        Map<UUID, UUID> itemIdsByLot = new HashMap<>();
        List<StockOutItemRequest> disposalItems = new ArrayList<>();
        for (StocktakeItem item : stocktakeItems.findByNoteIdOrderByItemId(stocktakeNoteId)) {
            BigDecimal quantity = stocktakeDisposalQuantity(stocktake, item);
            if (quantity.signum() <= 0) continue;
            itemIdsByLot.put(item.getLotId(), item.getItemId());
            disposalItems.add(new StockOutItemRequest(item.getProductId(), support.baseUnit(item.getProductId()).unitId(),
                    item.getLotId(), quantity));
        }
        if (disposalItems.isEmpty()) throw new AppException(WarehouseErrorCode.CONFLICT, "No stocktake items to dispose");
        return createPending(new StockOutCreateRequest(StockOutType.DISPOSAL, null, stocktake.getCode(), disposalItems),
                userId, stocktakeNoteId, itemIdsByLot);
    }

    private BigDecimal stocktakeDisposalQuantity(StocktakeNote stocktake, StocktakeItem item) {
        if (stocktake.getReviewedAt() != null) {
            return item.getDamagedQuantity() == null ? BigDecimal.ZERO : item.getDamagedQuantity();
        }
        // Existing submitted stocktakes still resolve shortages through their original disposal workflow.
        return item.getDiffQuantity() == null ? BigDecimal.ZERO : item.getDiffQuantity().negate();
    }

    @Override @Transactional
    public StockOutNote approveDisposal(UUID noteId) {
        WarehouseSupport.requireRole("MANAGER");
        StocktakeNote stocktake = notes.findStocktakeNoteId(noteId).map(this::lockStocktake).orElse(null);
        StockOutNote note = lockStockOut(noteId);
        requirePendingDisposal(note);
        if (note.getApprovedAt() != null || (stocktake != null && stocktake.getStatus() != StocktakeStatus.PENDING_DISPOSAL)) {
            throw new AppException(WarehouseErrorCode.CONFLICT);
        }
        List<StockOutItem> requests = items.findByNoteIdOrderByItemId(noteId);
        if (requests.isEmpty()) throw WarehouseSupport.error("Disposal must contain items");
        support.lockProducts(requests.stream().map(StockOutItem::getProductId).distinct().sorted().toList());
        Map<UUID, BigDecimal> quantities = new HashMap<>();
        requests.forEach(item -> quantities.merge(item.getLotId(), item.getQuantity(), BigDecimal::add));
        List<UUID> lotIds = quantities.keySet().stream().sorted().toList();
        if (stocktake == null) support.ensureLotsNotInDraftStocktake(lotIds);
        List<StockLot> lockedLots = lots.lockByLotIds(lotIds);
        if (lockedLots.size() != lotIds.size()) throw new AppException(WarehouseErrorCode.NOT_FOUND);
        LocalDateTime now = LocalDateTime.now();
        for (StockLot lot : lockedLots) {
            if (lot.getStatus() == LotStatus.EMPTY || lot.getStatus() == LotStatus.CANCELED
                    || unreservedQuantity(lot, now, null, note.getStocktakeNoteId()).compareTo(quantities.get(lot.getLotId())) < 0) {
                throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK);
            }
        }
        note.setApprovedBy(SecurityUtils.getCurrentUserId());
        note.setApprovedAt(now);
        note.setUpdatedAt(now);
        return notes.save(note);
    }

    @Override @Transactional
    public StockOutNote startDisposal(UUID noteId) {
        WarehouseSupport.requireRole("STAFF");
        notes.findStocktakeNoteId(noteId).ifPresent(this::lockStocktake);
        StockOutNote note = lockStockOut(noteId);
        requirePendingDisposal(note);
        if (note.getApprovedAt() == null || note.getStartedAt() != null) {
            throw new AppException(WarehouseErrorCode.CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        note.setStartedBy(SecurityUtils.getCurrentUserId());
        note.setStartedAt(now);
        note.setUpdatedAt(now);
        return notes.save(note);
    }

    private void requirePendingDisposal(StockOutNote note) {
        if (note.getType() != StockOutType.DISPOSAL || note.getStatus() != NoteStatus.PENDING) {
            throw new AppException(WarehouseErrorCode.CONFLICT);
        }
    }

    @Override @Transactional
    public StockOutNote complete(UUID noteId) {
        WarehouseSupport.requireWarehouseUser();
        UUID stocktakeId = notes.findStocktakeNoteId(noteId).orElse(null);
        StocktakeNote stocktake = stocktakeId == null ? null : lockStocktake(stocktakeId);
        StockOutNote stockOutNote = lockStockOut(noteId);
        if (stockOutNote.getType() == StockOutType.DISPOSAL) {
            WarehouseSupport.requireRole("STAFF");
            requirePendingDisposal(stockOutNote);
            if (stockOutNote.getApprovedAt() == null || stockOutNote.getStartedAt() == null) {
                throw new AppException(WarehouseErrorCode.CONFLICT);
            }
            if (!SecurityUtils.getCurrentUserId().equals(stockOutNote.getStartedBy())) {
                throw new AppException(WarehouseErrorCode.FORBIDDEN);
            }
            stockOutNote.setCompletedAt(LocalDateTime.now());
        }
        return postStockOut(stockOutNote, stocktake);
    }

    private StocktakeNote lockStocktake(UUID stocktakeNoteId) {
        return stocktakeNotes.lockById(stocktakeNoteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND));
    }

    private StockOutNote lockStockOut(UUID noteId) {
        return notes.lockById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND));
    }

    private StockOutNote postStockOut(StockOutNote stockOutNote, StocktakeNote stocktake) {
        UUID noteId = stockOutNote.getNoteId();
        if (stockOutNote.getStatus() != NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT, "Only PENDING stock-out notes can be completed");
        Map<UUID, StocktakeItem> disposalCounts = new HashMap<>();
        List<StocktakeItem> surplusItems = new ArrayList<>();
        if (stocktake != null) {
            if (stocktake.getStatus() != StocktakeStatus.PENDING_DISPOSAL) throw new AppException(WarehouseErrorCode.CONFLICT);
            for (StocktakeItem item : stocktakeItems.findByNoteIdOrderByItemId(stocktake.getNoteId())) {
                if (stocktakeDisposalQuantity(stocktake, item).signum() > 0) disposalCounts.put(item.getItemId(), item);
                if (stocktake.getReviewedAt() == null && item.getDiffQuantity() != null
                        && item.getDiffQuantity().signum() > 0) surplusItems.add(item);
            }
        }
        List<StockOutItem> requestedItems = items.findByNoteIdOrderByItemId(noteId);
        if (stocktake != null) {
            Set<UUID> requestedCountIds = new HashSet<>(requestedItems.stream().map(StockOutItem::getStocktakeItemId).toList());
            if (disposalCounts.isEmpty() || requestedItems.size() != requestedCountIds.size()
                    || !requestedCountIds.equals(disposalCounts.keySet())) {
                throw new AppException(WarehouseErrorCode.CONFLICT, "Disposal must cover every required stocktake item once");
            }
        }
        Set<UUID> productIds = new HashSet<>();
        requestedItems.forEach(item -> productIds.add(item.getProductId()));
        surplusItems.forEach(item -> productIds.add(item.getProductId()));
        List<InventoryProduct> lockedProducts = support.lockProducts(productIds.stream().sorted().toList());
        if (stockOutNote.getType() == StockOutType.SALE && requestedItems.stream().anyMatch(item -> item.getLotId() == null)) {
            lockedProducts.forEach(this::requireSaleProduct);
        }
        Map<UUID, BigDecimal> stockDeltas = new HashMap<>();
        items.deleteByNoteId(noteId);
        BigDecimal totalCost = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        for (StockOutItem requestedItem : requestedItems) {
            List<StockLot> eligibleLots;
            if (stockOutNote.getType() == StockOutType.SALE && requestedItem.getLotId() == null) eligibleLots = lots.findSaleableLots(requestedItem.getProductId(), LotStatus.STORAGE, today);
            else {
                if (stocktake == null && stockOutNote.getType() == StockOutType.DISPOSAL) support.ensureLotsNotInDraftStocktake(List.of(requestedItem.getLotId()));
                eligibleLots = lots.lockByLotIds(List.of(requestedItem.getLotId()));
            }
            if (stocktake != null) {
                StocktakeItem count = disposalCounts.get(requestedItem.getStocktakeItemId());
                if (count == null || !count.getLotId().equals(requestedItem.getLotId())
                        || stocktakeDisposalQuantity(stocktake, count).compareTo(requestedItem.getQuantity()) != 0
                        || eligibleLots.isEmpty()
                        || (stocktake.getReviewedAt() == null
                            && eligibleLots.getFirst().getCurrentQuantity().compareTo(count.getSystemQuantity()) != 0)) {
                    throw new AppException(WarehouseErrorCode.CONFLICT, "Stocktake disposal or lot quantity has changed");
                }
            }
            if (eligibleLots.isEmpty()) throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK, "No eligible lot for product " + requestedItem.getProductId());
            if (stockOutNote.getType() == StockOutType.SALE && requestedItem.getLotId() == null) {
                eligibleLots = lots.lockByLotIds(eligibleLots.stream().map(StockLot::getLotId).toList());
                eligibleLots.sort(Comparator.comparing(StockLot::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(StockLot::getCreatedAt).thenComparing(StockLot::getLotId));
                eligibleLots.removeIf(stockLot -> support.isLotInDraftStocktake(stockLot.getLotId()));
            }
            BigDecimal remainingQuantity = requestedItem.getQuantity();
            for (StockLot stockLot : eligibleLots) {
                if (!stockLot.getProductId().equals(requestedItem.getProductId()) || stockLot.getCurrentQuantity().signum() <= 0
                        || (stockOutNote.getType() == StockOutType.SALE && (stockLot.getStatus() != LotStatus.STORAGE || stockLot.getExpiryDate() != null && stockLot.getExpiryDate().isBefore(today)))
                        || (stockOutNote.getType() == StockOutType.DISPOSAL && (stockLot.getStatus() == LotStatus.EMPTY || stockLot.getStatus() == LotStatus.CANCELED))) continue;
                BigDecimal deductedQuantity = remainingQuantity.min(unreservedQuantity(stockLot, now, noteId, stockOutNote.getStocktakeNoteId()));
                if (deductedQuantity.signum() <= 0) continue;
                BigDecimal lineCost = WarehouseSupport.money(deductedQuantity.multiply(stockLot.getUnitCost()));
                if (lineCost.precision() - lineCost.scale() > 13) throw WarehouseSupport.error("Stock-out line cost exceeds the database precision limit");
                StockOutItem outputItem = new StockOutItem();
                outputItem.setNoteId(noteId); outputItem.setProductId(requestedItem.getProductId()); outputItem.setLotId(stockLot.getLotId());
                outputItem.setUnitId(requestedItem.getUnitId()); outputItem.setQuantity(deductedQuantity); outputItem.setUnitPrice(stockLot.getUnitCost()); outputItem.setTotalPrice(lineCost);
                outputItem.setStocktakeItemId(requestedItem.getStocktakeItemId());
                items.save(outputItem);
                stockLot.setCurrentQuantity(stockLot.getCurrentQuantity().subtract(deductedQuantity));
                stockLot.setStatus(stockLot.getCurrentQuantity().signum() == 0 ? LotStatus.EMPTY : stockLot.getExpiryDate() != null && stockLot.getExpiryDate().isBefore(today) ? LotStatus.EXPIRED : LotStatus.STORAGE);
                StockTransactionType transactionType = stockOutNote.getType() == StockOutType.SALE
                        ? StockTransactionType.SALE : StockTransactionType.DISPOSAL;
                stockTransactions.record(stockLot.getLotId(), stockLot.getProductId(), transactionType,
                        deductedQuantity.negate(), stockLot.getCurrentQuantity(), StockTransactionRefType.STOCK_OUT_NOTE,
                        stockOutNote.getNoteId());
                remainingQuantity = remainingQuantity.subtract(deductedQuantity);
                totalCost = totalCost.add(outputItem.getTotalPrice());
                if (remainingQuantity.signum() == 0) break;
            }
            if (remainingQuantity.signum() > 0) throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK, "Insufficient stock for product " + requestedItem.getProductId());
            stockDeltas.merge(requestedItem.getProductId(), requestedItem.getQuantity().negate(), BigDecimal::add);
        }
        if (!surplusItems.isEmpty()) {
            Map<UUID, StockLot> surplusLotsById = new HashMap<>();
            lots.lockByLotIds(surplusItems.stream().map(StocktakeItem::getLotId).toList())
                    .forEach(lot -> surplusLotsById.put(lot.getLotId(), lot));
            for (StocktakeItem surplus : surplusItems) {
                StocktakeSurplus.post(stocktake.getNoteId(), surplus, surplusLotsById.get(surplus.getLotId()),
                        today, stockTransactions, stockDeltas);
            }
        }
        support.adjustStock(stockDeltas);
        totalCost = WarehouseSupport.money(totalCost);
        if (totalCost.precision() - totalCost.scale() > 13) throw WarehouseSupport.error("Stock-out total cost exceeds the database precision limit");
        if (stocktake != null) {
            stocktake.setStatus(StocktakeStatus.COMPLETED);
            stocktake.setUpdatedAt(LocalDateTime.now());
        }
        stockOutNote.setTotalCost(totalCost);
        stockOutNote.setStatus(NoteStatus.COMPLETED);
        stockOutNote.setUpdatedAt(LocalDateTime.now());
        return notes.save(stockOutNote);
    }

    @Override @Transactional
    public StockOutNote cancel(UUID noteId, String reason) {
        WarehouseSupport.requireWarehouseUser();
        notes.findStocktakeNoteId(noteId).ifPresent(this::lockStocktake);
        StockOutNote stockOutNote = lockStockOut(noteId);
        if (stockOutNote.getType() == StockOutType.DISPOSAL) {
            WarehouseSupport.requireRole("MANAGER");
            if (stockOutNote.getApprovedAt() != null) throw new AppException(WarehouseErrorCode.CONFLICT);
            if (reason == null || reason.isBlank() || reason.length() > 1000) {
                throw WarehouseSupport.error("Enter a rejection reason of up to 1000 characters");
            }
            stockOutNote.setRejectionReason(reason.strip());
        }
        if (stockOutNote.getStatus() != NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT, "Only PENDING stock-out notes can be cancelled");
        stockOutNote.setStatus(NoteStatus.CANCELLED);
        stockOutNote.setUpdatedAt(LocalDateTime.now());
        return notes.save(stockOutNote);
    }

    @Override @Transactional(readOnly = true)
    public List<PendingLotDisposal> pendingExpiredLotDisposals() {
        WarehouseSupport.requireWarehouseUser();
        return items.findDisposalsForExpiredLots(NoteStatus.PENDING, StockOutType.DISPOSAL, LocalDate.now());
    }

    @Override
    @Transactional
    public StockOutNote disposeExpiredLot(UUID lotId) {
        WarehouseSupport.requireRole("STAFF");
        StockLot existingLot = lots.findById(lotId)
                .orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Lot not found"));
        UUID productId = existingLot.getProductId();
        products.lockInventoryProducts(List.of(productId));
        StockLot expiredLot = lots.lockByLotIds(List.of(lotId)).stream().findFirst()
                .orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Lot not found"));

        if (expiredLot.getExpiryDate() == null || !expiredLot.getExpiryDate().isBefore(LocalDate.now())) {
            throw WarehouseSupport.error("Only lots with an expiry date earlier than today can be disposed as expired");
        }
        if (expiredLot.getCurrentQuantity().signum() <= 0
                || expiredLot.getStatus() == LotStatus.EMPTY || expiredLot.getStatus() == LotStatus.CANCELED) {
            throw new AppException(WarehouseErrorCode.CONFLICT, "Lot has no remaining quantity to dispose");
        }

        InventoryUnit baseUnit = products.getActiveBaseInventoryUnit(productId);
        StockOutCreateRequest request = new StockOutCreateRequest(
                StockOutType.DISPOSAL,
                null,
                "EXPIRED: Hủy toàn bộ lô " + expiredLot.getLotCode(),
                List.of(new StockOutItemRequest(productId, baseUnit.unitId(), lotId, expiredLot.getCurrentQuantity())));
        return create(request);
    }
}
