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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class StockOutServiceImpl implements StockOutService {
    private final StockOutNoteRepository notes;
    private final StockOutItemRepository items;
    private final StockLotRepository lots;
    private final WarehouseSupport support;
    private final ProductService products;

    public StockOutServiceImpl(StockOutNoteRepository notes, StockOutItemRepository items, StockLotRepository lots,
                               WarehouseSupport support, ProductService products) {
        this.notes = notes; this.items = items; this.lots = lots; this.support = support; this.products = products;
    }

    @Override @Transactional(readOnly = true)
    public Page<StockOutNote> list(NoteStatus status, StockOutType type, Pageable pageable) {
        if (status != null && type != null) return notes.findByStatusAndType(status, type, pageable);
        if (status != null) return notes.findByStatus(status, pageable);
        if (type != null) return notes.findByType(type, pageable);
        return notes.findAll(pageable);
    }

    @Override @Transactional(readOnly = true)
    public StockOutDetailResponse get(UUID noteId) {
        StockOutNote stockOutNote = notes.findById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Stock-out note not found"));
        return new StockOutDetailResponse(stockOutNote, items.findByNoteIdOrderByItemId(noteId));
    }

    @Override @Transactional
    public StockOutNote create(StockOutCreateRequest request) {
        List<UUID> productIds = request.items().stream().map(StockOutItemRequest::productId).distinct().toList();
        Map<UUID, InventoryProduct> productsById = new HashMap<>();
        support.lockProducts(productIds).forEach(inventoryProduct -> productsById.put(inventoryProduct.productId(), inventoryProduct));
        StockOutNote stockOutNote = new StockOutNote();
        stockOutNote.setCode(WarehouseSupport.code("SOUT"));
        stockOutNote.setType(request.type());
        stockOutNote.setReferenceOrderId(request.referenceOrderId());
        stockOutNote.setNote(request.note());
        stockOutNote.setStatus(NoteStatus.PENDING);
        stockOutNote.setTotalCost(BigDecimal.ZERO);
        StockOutNote savedNote = notes.save(stockOutNote);
        List<StockOutItem> pendingItems = new ArrayList<>();
        for (StockOutItemRequest itemRequest : request.items()) {
            InventoryProduct inventoryProduct = productsById.get(itemRequest.productId());
            if (inventoryProduct == null) throw new AppException(WarehouseErrorCode.NOT_FOUND, "Product not found");
            InventoryUnit requestedUnit = support.unit(itemRequest.productId(), itemRequest.unitId());
            if (request.type() == StockOutType.SALE && itemRequest.lotId() != null) throw WarehouseSupport.error("SALE items use FEFO and must not specify lotId");
            if (request.type() == StockOutType.DISPOSAL && itemRequest.lotId() == null) throw WarehouseSupport.error("DISPOSAL items require lotId");
            if (itemRequest.lotId() != null) {
                StockLot requestedLot = lots.findById(itemRequest.lotId()).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Lot not found"));
                if (!requestedLot.getProductId().equals(inventoryProduct.productId())) throw WarehouseSupport.error("Lot does not belong to product");
            }
            InventoryUnit baseUnit = support.baseUnit(inventoryProduct.productId());
            StockOutItem pendingItem = new StockOutItem();
            pendingItem.setNoteId(savedNote.getNoteId());
            pendingItem.setProductId(inventoryProduct.productId());
            pendingItem.setLotId(itemRequest.lotId());
            pendingItem.setUnitId(baseUnit.unitId());
            BigDecimal baseQuantity = itemRequest.quantity().multiply(requestedUnit.conversionFactor()).setScale(2, java.math.RoundingMode.HALF_UP);
            if (baseQuantity.signum() <= 0 || baseQuantity.precision() - baseQuantity.scale() > 8) throw WarehouseSupport.error("Converted stock quantity exceeds the database precision limit");
            pendingItem.setQuantity(baseQuantity);
            pendingItem.setUnitPrice(BigDecimal.ZERO);
            pendingItem.setTotalPrice(BigDecimal.ZERO);
            pendingItems.add(pendingItem);
        }
        items.saveAll(pendingItems);
        return savedNote;
    }

    @Override
    @Transactional
    public StockOutNote processSaleOrder(UUID orderId, List<StockLine> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) throw WarehouseSupport.error("A sale order must contain items");

        StockOutNote existingNote = notes.lockByReferenceOrderIdAndType(orderId, StockOutType.SALE).orElse(null);
        if (existingNote != null) {
            if (existingNote.getStatus() == NoteStatus.COMPLETED) return existingNote;
            if (existingNote.getStatus() == NoteStatus.PENDING) return complete(existingNote.getNoteId());
            throw new AppException(WarehouseErrorCode.CONFLICT, "A cancelled sale stock-out already exists for this order");
        }

        Map<UUID, BigDecimal> baseQuantitiesByProduct = new TreeMap<>();
        Map<UUID, UUID> baseUnitsByProduct = new HashMap<>();
        for (StockLine orderItem : orderItems) {
            InventoryProduct inventoryProduct = products.getInventoryProduct(orderItem.productId());
            InventoryUnit selectedUnit = products.getActiveInventoryUnit(orderItem.productId(), orderItem.productUnitId());
            BigDecimal baseQuantity = BigDecimal.valueOf(orderItem.quantity()).multiply(selectedUnit.conversionFactor())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            if (baseQuantity.signum() <= 0 || baseQuantity.precision() - baseQuantity.scale() > 8) {
                throw WarehouseSupport.error("Converted stock quantity exceeds the database precision limit");
            }
            expandSaleProduct(inventoryProduct.productId(), baseQuantity, new HashSet<>(),
                    baseQuantitiesByProduct, baseUnitsByProduct);
        }

        List<StockOutItemRequest> expandedItems = baseQuantitiesByProduct.entrySet().stream()
                .map(entry -> new StockOutItemRequest(entry.getKey(), baseUnitsByProduct.get(entry.getKey()), null, entry.getValue()))
                .toList();
        StockOutCreateRequest stockOutRequest = new StockOutCreateRequest(StockOutType.SALE, orderId,
                "Sale order " + orderId, expandedItems);
        StockOutNote pendingNote = create(stockOutRequest);
        return complete(pendingNote.getNoteId());
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
            stockDeltas.merge(stockOutItem.getProductId(), stockOutItem.getQuantity(), BigDecimal::add);
        }
        support.adjustStock(stockDeltas);
        stockOutNote.setStatus(NoteStatus.CANCELLED);
        stockOutNote.setUpdatedAt(LocalDateTime.now());
        notes.save(stockOutNote);
    }

    private void expandSaleProduct(UUID productId, BigDecimal quantity, Set<UUID> productsInPath,
                                   Map<UUID, BigDecimal> baseQuantitiesByProduct,
                                   Map<UUID, UUID> baseUnitsByProduct) {
        if (!productsInPath.add(productId)) throw WarehouseSupport.error("Combo composition contains a cycle");
        InventoryProduct inventoryProduct = products.getInventoryProduct(productId);
        if (inventoryProduct.combo()) {
            List<ComboItemRequest> comboItems = products.getComboItems(productId);
            if (comboItems.isEmpty()) throw WarehouseSupport.error("Combo has no components: " + productId);
            for (ComboItemRequest comboItem : comboItems) {
                BigDecimal componentQuantity = quantity.multiply(comboItem.quantity())
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                expandSaleProduct(comboItem.productId(), componentQuantity, productsInPath,
                        baseQuantitiesByProduct, baseUnitsByProduct);
            }
        } else {
            InventoryUnit baseUnit = products.getActiveBaseInventoryUnit(productId);
            baseQuantitiesByProduct.merge(productId, quantity, BigDecimal::add);
            baseUnitsByProduct.put(productId, baseUnit.unitId());
        }
        productsInPath.remove(productId);
    }

    @Override @Transactional
    public StockOutNote complete(UUID noteId) {
        StockOutNote stockOutNote = notes.lockById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Stock-out note not found"));
        if (stockOutNote.getStatus() != NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT, "Only PENDING stock-out notes can be completed");
        List<StockOutItem> requestedItems = items.findByNoteIdOrderByItemId(noteId);
        List<UUID> productIds = requestedItems.stream().map(StockOutItem::getProductId).distinct().toList();
        support.lockProducts(productIds);
        Map<UUID, BigDecimal> stockDeltas = new HashMap<>();
        items.deleteByNoteId(noteId);
        BigDecimal totalCost = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        for (StockOutItem requestedItem : requestedItems) {
            List<StockLot> eligibleLots;
            if (stockOutNote.getType() == StockOutType.SALE) eligibleLots = lots.findSaleableLots(requestedItem.getProductId(), LotStatus.STORAGE, today);
            else {
                support.ensureLotsNotInDraftStocktake(List.of(requestedItem.getLotId()));
                eligibleLots = lots.lockByLotIds(List.of(requestedItem.getLotId()));
            }
            if (eligibleLots.isEmpty()) throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK, "No eligible lot for product " + requestedItem.getProductId());
            if (stockOutNote.getType() == StockOutType.SALE) {
                eligibleLots = lots.lockByLotIds(eligibleLots.stream().map(StockLot::getLotId).toList());
                eligibleLots.sort(Comparator.comparing(StockLot::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(StockLot::getCreatedAt).thenComparing(StockLot::getLotId));
                eligibleLots.removeIf(stockLot -> support.isLotInDraftStocktake(stockLot.getLotId()));
            }
            BigDecimal remainingQuantity = requestedItem.getQuantity();
            for (StockLot stockLot : eligibleLots) {
                if (!stockLot.getProductId().equals(requestedItem.getProductId()) || stockLot.getCurrentQuantity().signum() <= 0
                        || (stockOutNote.getType() == StockOutType.SALE && (stockLot.getStatus() != LotStatus.STORAGE || stockLot.getExpiryDate() != null && stockLot.getExpiryDate().isBefore(today)))
                        || (stockOutNote.getType() == StockOutType.DISPOSAL && (stockLot.getStatus() == LotStatus.EMPTY || stockLot.getStatus() == LotStatus.CANCELED))) continue;
                BigDecimal deductedQuantity = remainingQuantity.min(stockLot.getCurrentQuantity());
                if (deductedQuantity.signum() <= 0) continue;
                BigDecimal lineCost = WarehouseSupport.money(deductedQuantity.multiply(stockLot.getUnitCost()));
                if (lineCost.precision() - lineCost.scale() > 13) throw WarehouseSupport.error("Stock-out line cost exceeds the database precision limit");
                StockOutItem outputItem = new StockOutItem();
                outputItem.setNoteId(noteId); outputItem.setProductId(requestedItem.getProductId()); outputItem.setLotId(stockLot.getLotId());
                outputItem.setUnitId(requestedItem.getUnitId()); outputItem.setQuantity(deductedQuantity); outputItem.setUnitPrice(stockLot.getUnitCost()); outputItem.setTotalPrice(lineCost);
                items.save(outputItem);
                stockLot.setCurrentQuantity(stockLot.getCurrentQuantity().subtract(deductedQuantity));
                stockLot.setStatus(stockLot.getCurrentQuantity().signum() == 0 ? LotStatus.EMPTY : stockLot.getExpiryDate() != null && stockLot.getExpiryDate().isBefore(today) ? LotStatus.EXPIRED : LotStatus.STORAGE);
                remainingQuantity = remainingQuantity.subtract(deductedQuantity);
                totalCost = totalCost.add(outputItem.getTotalPrice());
                if (remainingQuantity.signum() == 0) break;
            }
            if (remainingQuantity.signum() > 0) throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK, "Insufficient stock for product " + requestedItem.getProductId());
            stockDeltas.merge(requestedItem.getProductId(), requestedItem.getQuantity().negate(), BigDecimal::add);
        }
        support.adjustStock(stockDeltas);
        totalCost = WarehouseSupport.money(totalCost);
        if (totalCost.precision() - totalCost.scale() > 13) throw WarehouseSupport.error("Stock-out total cost exceeds the database precision limit");
        stockOutNote.setTotalCost(totalCost);
        stockOutNote.setStatus(NoteStatus.COMPLETED);
        stockOutNote.setUpdatedAt(LocalDateTime.now());
        return notes.save(stockOutNote);
    }

    @Override @Transactional
    public StockOutNote cancel(UUID noteId) {
        StockOutNote stockOutNote = notes.lockById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Stock-out note not found"));
        if (stockOutNote.getStatus() != NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT, "Only PENDING stock-out notes can be cancelled");
        stockOutNote.setStatus(NoteStatus.CANCELLED);
        stockOutNote.setUpdatedAt(LocalDateTime.now());
        return notes.save(stockOutNote);
    }
}
