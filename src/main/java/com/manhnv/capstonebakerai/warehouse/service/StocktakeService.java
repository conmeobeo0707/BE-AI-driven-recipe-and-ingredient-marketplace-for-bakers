package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
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
public class StocktakeService {
    private final StocktakeNoteRepository notes; private final StocktakeItemRepository items;
    private final StockLotRepository lots; private final WarehouseSupport support;
    public StocktakeService(StocktakeNoteRepository notes,StocktakeItemRepository items,StockLotRepository lots,WarehouseSupport support){this.notes=notes;this.items=items;this.lots=lots;this.support=support;}
    @Transactional(readOnly = true)
    public Page<StocktakeNote> list(StocktakeStatus status, Pageable pageable) {
        return status==null?notes.findAll(pageable):notes.findByStatus(status,pageable);
    }
    @Transactional(readOnly = true)
    public StocktakeDetailResponse get(UUID noteId) {
        StocktakeNote stocktakeNote=notes.findById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake note not found"));
        return detail(stocktakeNote,items.findByNoteIdOrderByItemId(noteId));
    }
    @Transactional
    public StocktakeDetailResponse create(StocktakeCreateRequest request) {
        List<UUID> productIds=request.productIds().stream().distinct().sorted().toList();
        if(productIds.size()!=request.productIds().size()) throw WarehouseSupport.error("Duplicate productIds are not allowed");
        support.lockProducts(productIds);
        List<StockLot> foundLots=lots.findByProductIdInAndStatusInOrderByProductIdAscExpiryDateAscCreatedAtAsc(
                productIds,List.of(LotStatus.STORAGE,LotStatus.EXPIRED,LotStatus.EMPTY));
        Map<UUID,List<StockLot>> lotsByProduct=new HashMap<>();
        foundLots.forEach(stockLot->lotsByProduct.computeIfAbsent(stockLot.getProductId(),productId->new ArrayList<>()).add(stockLot));
        for(UUID productId:productIds) if(!lotsByProduct.containsKey(productId))
            throw WarehouseSupport.error("Product has no stock lots to count: "+productId);
        List<UUID> lotIds=foundLots.stream().map(StockLot::getLotId).toList();
        List<StockLot> lockedLots=lots.lockByLotIds(lotIds);
        support.ensureLotsNotInDraftStocktake(lotIds);
        StocktakeNote stocktakeNote=new StocktakeNote(); stocktakeNote.setCode(WarehouseSupport.code("STK")); stocktakeNote.setStocktakeDate(LocalDateTime.now()); stocktakeNote.setCreatedBy(SecurityUtils.getCurrentUserId()); stocktakeNote.setHandleBy(request.handleBy()); stocktakeNote.setStatus(StocktakeStatus.DRAFT);
        StocktakeNote savedNote=notes.save(stocktakeNote); List<StocktakeItem> stocktakeItems=new ArrayList<>();
        for(StockLot stockLot:lockedLots) {
            if(stockLot.getStatus()==LotStatus.CANCELED) throw WarehouseSupport.error("Cancelled lots cannot be stocktaken");
            StocktakeItem stocktakeItem=new StocktakeItem();stocktakeItem.setNoteId(savedNote.getNoteId());stocktakeItem.setProductId(stockLot.getProductId());stocktakeItem.setLotId(stockLot.getLotId());stocktakeItem.setSystemQuantity(stockLot.getCurrentQuantity());stocktakeItem.setUnitCost(stockLot.getUnitCost());stocktakeItems.add(stocktakeItem);
        }
        items.saveAll(stocktakeItems);return detail(savedNote,stocktakeItems);
    }
    @Transactional
    public StocktakeDetailResponse count(UUID noteId,StocktakeCountRequest request) {
        StocktakeNote stocktakeNote=notes.lockById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake note not found"));
        if(stocktakeNote.getStatus()!=StocktakeStatus.DRAFT) throw new AppException(WarehouseErrorCode.CONFLICT,"Only DRAFT stocktakes accept counts");
        List<StocktakeItem> stocktakeItems=items.findByNoteIdOrderByItemId(noteId); Map<UUID,StocktakeItem> itemsById=new HashMap<>();stocktakeItems.forEach(stocktakeItem->itemsById.put(stocktakeItem.getItemId(),stocktakeItem));
        Set<UUID> countedItemIds=new HashSet<>();
        for(StocktakeCountRequest.CountedProduct productCount:request.products()) {
            Set<UUID> productLots=new HashSet<>();
            for(StocktakeCountRequest.CountedLot lotCount:productCount.lots()) {
                if(!productLots.add(lotCount.itemId())||!countedItemIds.add(lotCount.itemId()))throw WarehouseSupport.error("Duplicate stocktake item counts are not allowed");
                StocktakeItem stocktakeItem=itemsById.get(lotCount.itemId());
                if(stocktakeItem==null||!stocktakeItem.getProductId().equals(productCount.productId()))throw WarehouseSupport.error("Stocktake item is not part of the specified product in this note");
                stocktakeItem.setActualQuantity(lotCount.actualQuantity());stocktakeItem.setReason(lotCount.reason());stocktakeItem.setNote(lotCount.note());
            }
        }
        return detail(stocktakeNote,items.saveAll(stocktakeItems));
    }
    @Transactional
    public StocktakeDetailResponse complete(UUID noteId) {
        StocktakeNote stocktakeNote=notes.lockById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake note not found"));
        if(stocktakeNote.getStatus()!=StocktakeStatus.DRAFT)throw new AppException(WarehouseErrorCode.CONFLICT,"Only DRAFT stocktakes can be completed");
        List<StocktakeItem> stocktakeItems=items.findByNoteIdOrderByItemId(noteId);if(stocktakeItems.isEmpty())throw WarehouseSupport.error("Stocktake must contain at least one lot");
        List<UUID> productIds=stocktakeItems.stream().map(StocktakeItem::getProductId).distinct().sorted().toList();support.lockProducts(productIds);
        List<StockLot> lockedLots=lots.lockByLotIds(stocktakeItems.stream().map(StocktakeItem::getLotId).toList());Map<UUID,StockLot> lotsById=new HashMap<>();lockedLots.forEach(stockLot->lotsById.put(stockLot.getLotId(),stockLot));
        Map<UUID,BigDecimal> stockDeltas=new HashMap<>();
        for(StocktakeItem stocktakeItem:stocktakeItems) {
            if(stocktakeItem.getActualQuantity()==null)throw WarehouseSupport.error("Enter actual quantity for every lot before completing");
            StockLot stockLot=lotsById.get(stocktakeItem.getLotId()); if(stockLot==null)throw new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake lot no longer exists");
            if(stockLot.getCurrentQuantity().compareTo(stocktakeItem.getSystemQuantity())!=0)throw new AppException(WarehouseErrorCode.CONFLICT,"Lot changed during stocktake; cancel and create a new stocktake");
            BigDecimal difference=stocktakeItem.getActualQuantity().subtract(stocktakeItem.getSystemQuantity()); if(difference.signum()!=0&&(stocktakeItem.getReason()==null||stocktakeItem.getReason().isBlank()))throw WarehouseSupport.error("Reason is required when counted quantity differs");
            BigDecimal costAmount=WarehouseSupport.money(difference.multiply(stocktakeItem.getUnitCost()));if(costAmount.precision()-costAmount.scale()>13)throw WarehouseSupport.error("Stocktake cost amount exceeds the database precision limit");
            stocktakeItem.setDiffQuantity(difference);stocktakeItem.setCostAmount(costAmount);
            stockDeltas.merge(stocktakeItem.getProductId(),difference,BigDecimal::add);
            stockLot.setCurrentQuantity(stocktakeItem.getActualQuantity());stockLot.setStatus(stockLot.getCurrentQuantity().signum()==0?LotStatus.EMPTY:stockLot.getExpiryDate()!=null&&stockLot.getExpiryDate().isBefore(LocalDate.now())?LotStatus.EXPIRED:LotStatus.STORAGE);
        }
        items.saveAll(stocktakeItems);support.adjustStock(stockDeltas);stocktakeNote.setStatus(StocktakeStatus.COMPLETED);stocktakeNote.setUpdatedAt(LocalDateTime.now());notes.save(stocktakeNote);return detail(stocktakeNote,stocktakeItems);
    }
    @Transactional
    public StocktakeNote cancel(UUID noteId) {
        StocktakeNote stocktakeNote=notes.lockById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake note not found"));
        if(stocktakeNote.getStatus()!=StocktakeStatus.DRAFT)throw new AppException(WarehouseErrorCode.CONFLICT,"Only DRAFT stocktakes can be cancelled");
        stocktakeNote.setStatus(StocktakeStatus.CANCELLED);stocktakeNote.setUpdatedAt(LocalDateTime.now());return notes.save(stocktakeNote);
    }
    private StocktakeDetailResponse detail(StocktakeNote note,List<StocktakeItem> rows) {
        Map<UUID,List<StocktakeItem>> grouped=new LinkedHashMap<>();
        rows.stream().sorted(Comparator.comparing(StocktakeItem::getProductId).thenComparing(StocktakeItem::getLotId))
                .forEach(stocktakeItem->grouped.computeIfAbsent(stocktakeItem.getProductId(),productId->new ArrayList<>()).add(stocktakeItem));
        List<StocktakeDetailResponse.ProductLots> products=grouped.entrySet().stream()
                .map(entry->new StocktakeDetailResponse.ProductLots(entry.getKey(),List.copyOf(entry.getValue()))).toList();
        return new StocktakeDetailResponse(note,products);
    }
}
