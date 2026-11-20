package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.warehouse.dto.*;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
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
public class StocktakeServiceImpl implements StocktakeService {
    private final StocktakeNoteRepository notes;
    private final StocktakeItemRepository items;
    private final StockLotRepository lots;
    private final WarehouseSupport support;
    private final StockOutNoteRepository stockOutNotes;
    private final UserRepository users;

    public StocktakeServiceImpl(StocktakeNoteRepository notes, StocktakeItemRepository items, StockLotRepository lots,
                                WarehouseSupport support, StockOutNoteRepository stockOutNotes,
                                UserRepository users) {
        this.notes = notes; this.items = items; this.lots = lots; this.support = support;
        this.stockOutNotes = stockOutNotes; this.users = users;
    }

    @Override @Transactional(readOnly = true)
    public Page<StocktakeNote> list(StocktakeStatus status, Pageable pageable) {
        WarehouseSupport.requireWarehouseUser();
        return notes.findVisible(WarehouseSupport.hasRole("MANAGER"), SecurityUtils.getCurrentUserId(),
                status == null ? null : status.name(), pageable);
    }

    @Override @Transactional(readOnly = true)
    public StocktakeDetailResponse get(UUID noteId) {
        StocktakeNote stocktakeNote = notes.findById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Stocktake note not found"));
        if (!WarehouseSupport.hasRole("MANAGER")) requireAssignee(stocktakeNote);
        return detail(stocktakeNote, items.findByNoteIdOrderByItemId(noteId));
    }

    @Override @Transactional
    public StocktakeDetailResponse create(StocktakeCreateRequest request) {
        WarehouseSupport.requireRole("MANAGER");
        if (request.handleBy() != null) validateStaff(request.handleBy());
        List<UUID> productIds = request.productIds().stream().distinct().sorted().toList();
        if (productIds.size() != request.productIds().size()) throw WarehouseSupport.error("Duplicate productIds are not allowed");
        support.lockProducts(productIds);
        List<StockLot> foundLots = lots.findByProductIdInAndStatusInOrderByProductIdAscExpiryDateAscCreatedAtAsc(productIds, List.of(LotStatus.STORAGE, LotStatus.EXPIRED, LotStatus.EMPTY));
        Map<UUID, List<StockLot>> lotsByProduct = new HashMap<>();
        foundLots.forEach(stockLot -> lotsByProduct.computeIfAbsent(stockLot.getProductId(), productId -> new ArrayList<>()).add(stockLot));
        for (UUID productId : productIds) if (!lotsByProduct.containsKey(productId)) throw WarehouseSupport.error("Product has no stock lots to count: " + productId);
        List<UUID> lotIds = foundLots.stream().map(StockLot::getLotId).toList();
        List<StockLot> lockedLots = lots.lockByLotIds(lotIds);
        support.ensureLotsNotInDraftStocktake(lotIds);
        StocktakeNote stocktakeNote = new StocktakeNote();
        stocktakeNote.setCode(WarehouseSupport.code("STK"));
        stocktakeNote.setStocktakeDate(LocalDateTime.now());
        stocktakeNote.setCreatedBy(SecurityUtils.getCurrentUserId());
        stocktakeNote.setHandleBy(request.handleBy());
        stocktakeNote.setStatus(StocktakeStatus.DRAFT);
        StocktakeNote savedNote = notes.save(stocktakeNote);
        List<StocktakeItem> stocktakeItems = new ArrayList<>();
        for (StockLot stockLot : lockedLots) {
            if (stockLot.getStatus() == LotStatus.CANCELED) throw WarehouseSupport.error("Cancelled lots cannot be stocktaken");
            StocktakeItem stocktakeItem = new StocktakeItem();
            stocktakeItem.setNoteId(savedNote.getNoteId()); stocktakeItem.setProductId(stockLot.getProductId()); stocktakeItem.setLotId(stockLot.getLotId());
            stocktakeItem.setSystemQuantity(stockLot.getCurrentQuantity()); stocktakeItem.setUnitCost(stockLot.getUnitCost());
            stocktakeItems.add(stocktakeItem);
        }
        items.saveAll(stocktakeItems);
        return detail(savedNote, stocktakeItems);
    }

    @Override @Transactional(readOnly = true)
    public List<StaffAssignee> assignees() {
        WarehouseSupport.requireRole("MANAGER");
        return users.findByRoleInAndStatusOrderByCreatedAtDescUserIdAsc(List.of(Role.STAFF), UserStatus.ACTIVE)
                .stream().map(user -> new StaffAssignee(user.getUserId(), user.getFullName())).toList();
    }

    @Override @Transactional
    public StocktakeDetailResponse assign(UUID noteId, UUID staffId) {
        WarehouseSupport.requireRole("MANAGER");
        StocktakeNote note = notes.lockById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND));
        if (note.getStatus() != StocktakeStatus.DRAFT) throw new AppException(WarehouseErrorCode.CONFLICT);
        validateStaff(staffId);
        note.setHandleBy(staffId);
        note.setUpdatedAt(LocalDateTime.now());
        return detail(note, items.findByNoteIdOrderByItemId(noteId));
    }

    private void validateStaff(UUID staffId) {
        if (users.findById(staffId).filter(user -> user.getRole() == Role.STAFF
                && user.getStatus() == UserStatus.ACTIVE).isEmpty()) {
            throw WarehouseSupport.error("Assign an active staff member");
        }
    }

    private void requireAssignee(StocktakeNote note) {
        WarehouseSupport.requireRole("STAFF");
        if (!Objects.equals(note.getHandleBy(), SecurityUtils.getCurrentUserId())) {
            throw new AppException(WarehouseErrorCode.FORBIDDEN);
        }
    }

    @Override @Transactional
    public StocktakeDetailResponse count(UUID noteId, StocktakeCountRequest request) {
        StocktakeNote stocktakeNote = notes.lockById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Stocktake note not found"));
        requireAssignee(stocktakeNote);
        if (stocktakeNote.getStatus() != StocktakeStatus.ASSIGNED) throw new AppException(WarehouseErrorCode.CONFLICT, "Only ASSIGNED stocktakes accept counts");
        List<StocktakeItem> stocktakeItems = items.findByNoteIdOrderByItemId(noteId);
        updateCounts(stocktakeItems, request);
        return detail(stocktakeNote, stocktakeItems);
    }

    private void updateCounts(List<StocktakeItem> stocktakeItems, StocktakeCountRequest request) {
        Map<UUID, StocktakeItem> itemsById = new HashMap<>();
        stocktakeItems.forEach(stocktakeItem -> itemsById.put(stocktakeItem.getItemId(), stocktakeItem));
        Set<UUID> countedItemIds = new HashSet<>();
        for (StocktakeCountRequest.CountedProduct productCount : request.products()) {
            Set<UUID> productLotItemIds = new HashSet<>();
            for (StocktakeCountRequest.CountedLot lotCount : productCount.lots()) {
                if (!productLotItemIds.add(lotCount.itemId()) || !countedItemIds.add(lotCount.itemId())) throw WarehouseSupport.error("Duplicate stocktake item counts are not allowed");
                StocktakeItem stocktakeItem = itemsById.get(lotCount.itemId());
                if (stocktakeItem == null || !stocktakeItem.getProductId().equals(productCount.productId())) throw WarehouseSupport.error("Stocktake item is not part of the specified product in this note");
                stocktakeItem.setActualQuantity(lotCount.actualQuantity()); stocktakeItem.setReason(lotCount.reason()); stocktakeItem.setNote(lotCount.note());
            }
        }
    }

    @Override @Transactional
    public StocktakeDetailResponse complete(UUID noteId, StocktakeCountRequest request) {
        StocktakeNote stocktakeNote = notes.lockById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Stocktake note not found"));
        requireAssignee(stocktakeNote);
        if (stocktakeNote.getStatus() != StocktakeStatus.ASSIGNED) throw new AppException(WarehouseErrorCode.CONFLICT, "Only ASSIGNED stocktakes can be completed");
        List<StocktakeItem> stocktakeItems = items.findByNoteIdOrderByItemId(noteId);
        if (request != null) updateCounts(stocktakeItems, request);
        if (stocktakeItems.isEmpty()) throw WarehouseSupport.error("Stocktake must contain at least one lot");
        List<UUID> productIds = stocktakeItems.stream().map(StocktakeItem::getProductId).distinct().sorted().toList();
        support.lockProducts(productIds);
        List<StockLot> lockedLots = lots.lockByLotIds(stocktakeItems.stream().map(StocktakeItem::getLotId).toList());
        Map<UUID, StockLot> lotsById = new HashMap<>();
        lockedLots.forEach(stockLot -> lotsById.put(stockLot.getLotId(), stockLot));
        boolean hasShortage = false;
        for (StocktakeItem stocktakeItem : stocktakeItems) {
            if (stocktakeItem.getActualQuantity() == null) throw WarehouseSupport.error("Enter actual quantity for every lot before completing");
            StockLot stockLot = lotsById.get(stocktakeItem.getLotId());
            if (stockLot == null) throw new AppException(WarehouseErrorCode.NOT_FOUND, "Stocktake lot no longer exists");
            if (stockLot.getCurrentQuantity().compareTo(stocktakeItem.getSystemQuantity()) != 0) throw new AppException(WarehouseErrorCode.CONFLICT, "Lot changed during stocktake; cancel and create a new stocktake");
            BigDecimal difference = stocktakeItem.getActualQuantity().subtract(stocktakeItem.getSystemQuantity());
            if (difference.signum() != 0 && (stocktakeItem.getReason() == null || stocktakeItem.getReason().isBlank())) throw WarehouseSupport.error("Reason is required when counted quantity differs");
            BigDecimal costAmount = WarehouseSupport.money(difference.multiply(stocktakeItem.getUnitCost()));
            if (costAmount.precision() - costAmount.scale() > 13) throw WarehouseSupport.error("Stocktake cost amount exceeds the database precision limit");
            stocktakeItem.setDiffQuantity(difference); stocktakeItem.setCostAmount(costAmount);
            hasShortage |= difference.signum() < 0;
        }
        stocktakeNote.setCountsSubmittedAt(LocalDateTime.now());
        if (!hasShortage) stocktakeNote.setStatus(StocktakeStatus.COMPLETED);
        stocktakeNote.setUpdatedAt(LocalDateTime.now());
        notes.save(stocktakeNote);
        return detail(stocktakeNote, stocktakeItems);
    }

    @Override @Transactional
    public StocktakeNote cancel(UUID noteId) {
        StocktakeNote stocktakeNote = notes.lockById(noteId).orElseThrow(() -> new AppException(WarehouseErrorCode.NOT_FOUND, "Stocktake note not found"));
        WarehouseSupport.requireRole("MANAGER");
        if (stocktakeNote.getStatus() == StocktakeStatus.COMPLETED || stocktakeNote.getStatus() == StocktakeStatus.CANCELLED) {
            throw new AppException(WarehouseErrorCode.CONFLICT);
        }
        stockOutNotes.findByStocktakeNoteIdAndStatus(noteId, NoteStatus.PENDING).ifPresent(disposal -> {
            disposal.setStatus(NoteStatus.CANCELLED);
            disposal.setUpdatedAt(LocalDateTime.now());
        });
        stocktakeNote.setStatus(StocktakeStatus.CANCELLED);
        stocktakeNote.setUpdatedAt(LocalDateTime.now());
        return notes.save(stocktakeNote);
    }

    private StocktakeDetailResponse detail(StocktakeNote stocktakeNote, List<StocktakeItem> stocktakeItems) {
        Map<UUID, List<StocktakeItem>> itemsByProductId = new LinkedHashMap<>();
        stocktakeItems.stream().sorted(Comparator.comparing(StocktakeItem::getProductId).thenComparing(StocktakeItem::getLotId))
                .forEach(stocktakeItem -> itemsByProductId.computeIfAbsent(stocktakeItem.getProductId(), productId -> new ArrayList<>()).add(stocktakeItem));
        List<StocktakeDetailResponse.ProductLots> products = itemsByProductId.entrySet().stream()
                .map(entry -> new StocktakeDetailResponse.ProductLots(entry.getKey(), List.copyOf(entry.getValue()))).toList();
        String pendingDisposalCode = stocktakeNote.getStatus() == StocktakeStatus.PENDING_DISPOSAL
                ? stockOutNotes.findByStocktakeNoteIdAndStatus(stocktakeNote.getNoteId(), NoteStatus.PENDING)
                    .map(StockOutNote::getCode).orElse(null)
                : null;
        return new StocktakeDetailResponse(stocktakeNote, products, pendingDisposalCode);
    }
}
