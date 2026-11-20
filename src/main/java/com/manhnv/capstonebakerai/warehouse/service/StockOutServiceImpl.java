package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.product.dtos.ComboItemRequest;
import com.manhnv.capstonebakerai.product.dtos.InventoryProduct;
import com.manhnv.capstonebakerai.product.dtos.InventoryUnit;
import com.manhnv.capstonebakerai.product.service.ProductService;
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

    public StockOutServiceImpl(StockOutNoteRepository notes, StockOutItemRepository items, StockLotRepository lots,
                               WarehouseSupport support, ProductService products,
                               StockTransactionService stockTransactions, StocktakeNoteRepository stocktakeNotes,
                               StocktakeItemRepository stocktakeItems, UserRepository users) {
        this.notes = notes; this.items = items; this.lots = lots; this.support = support; this.products = products;
        this.stockTransactions = stockTransactions;
        this.stocktakeNotes = stocktakeNotes;
        this.stocktakeItems = stocktakeItems;
        this.users = users;
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
        return createPending(request, SecurityUtils.getCurrentUserId(), null, Map.of());
    }

    private StockOutNote createPending(StockOutCreateRequest request, UUID creator, UUID stocktakeNoteId,
                                       Map<UUID, UUID> stocktakeItemsByLot) {
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
            if (itemRequest.lotId() != null) pendingItem.setStocktakeItemId(stocktakeItemsByLot.get(itemRequest.lotId()));
            pendingItem.setUnitId(baseUnit.unitId());
            BigDecimal baseQuantity = (stocktakeNoteId == null
                    ? itemRequest.quantity().multiply(requestedUnit.conversionFactor()) : itemRequest.quantity()).setScale(2, java.math.RoundingMode.HALF_UP);
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
            if (existingNote.getStatus() == NoteStatus.PENDING) return postStockOut(existingNote, null);
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
        StockOutNote pendingNote = createPending(stockOutRequest, null, null, Map.of());
        return postStockOut(pendingNote, null);
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
        List<StockOutItemRequest> shortages = new ArrayList<>();
        for (StocktakeItem item : stocktakeItems.findByNoteIdOrderByItemId(stocktakeNoteId)) {
            if (item.getDiffQuantity() == null || item.getDiffQuantity().signum() >= 0) continue;
            itemIdsByLot.put(item.getLotId(), item.getItemId());
            shortages.add(new StockOutItemRequest(item.getProductId(), support.baseUnit(item.getProductId()).unitId(),
                    item.getLotId(), item.getDiffQuantity().negate()));
        }
        if (shortages.isEmpty()) throw new AppException(WarehouseErrorCode.CONFLICT, "No shortages to dispose");
        return createPending(new StockOutCreateRequest(StockOutType.DISPOSAL, null, stocktake.getCode(), shortages),
                userId, stocktakeNoteId, itemIdsByLot);
    }

    @Override @Transactional
    public StockOutNote complete(UUID noteId) {
        WarehouseSupport.requireWarehouseUser();
        UUID stocktakeId = notes.findStocktakeNoteId(noteId).orElse(null);
        StocktakeNote stocktake = stocktakeId == null ? null : lockStocktake(stocktakeId);
        StockOutNote stockOutNote = lockStockOut(noteId);
        if (stockOutNote.getType() == StockOutType.DISPOSAL) {
            WarehouseSupport.requireRole("MANAGER");
            stockOutNote.setApprovedBy(SecurityUtils.getCurrentUserId());
            stockOutNote.setApprovedAt(LocalDateTime.now());
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
        Map<UUID, StocktakeItem> shortageItems = new HashMap<>();
        if (stocktake != null) {
            if (stocktake.getStatus() != StocktakeStatus.PENDING_DISPOSAL) throw new AppException(WarehouseErrorCode.CONFLICT);
            stocktakeItems.findByNoteIdOrderByItemId(stocktake.getNoteId())
                    .forEach(item -> shortageItems.put(item.getItemId(), item));
        }
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
                if (stocktake == null) support.ensureLotsNotInDraftStocktake(List.of(requestedItem.getLotId()));
                eligibleLots = lots.lockByLotIds(List.of(requestedItem.getLotId()));
            }
            if (stocktake != null) {
                StocktakeItem shortage = shortageItems.get(requestedItem.getStocktakeItemId());
                if (shortage == null || !shortage.getLotId().equals(requestedItem.getLotId())
                        || shortage.getDiffQuantity() == null
                        || shortage.getDiffQuantity().negate().compareTo(requestedItem.getQuantity()) != 0
                        || eligibleLots.isEmpty()
                        || eligibleLots.getFirst().getCurrentQuantity().compareTo(shortage.getSystemQuantity()) != 0) {
                    throw new AppException(WarehouseErrorCode.CONFLICT, "Stocktake shortage or lot quantity has changed");
                }
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

    @Override
    @Transactional
    public StockOutNote disposeExpiredLot(UUID lotId) {
        WarehouseSupport.requireWarehouseUser();
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
