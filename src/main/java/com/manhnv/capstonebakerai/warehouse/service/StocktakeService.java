package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.product.entities.Product;
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
    public Page<StocktakeNote> list(StocktakeStatus status, Pageable pageable) { return notes.search(status,pageable); }
    @Transactional(readOnly = true)
    public StocktakeDetailResponse get(UUID id) {
        StocktakeNote note=notes.findById(id).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake note not found"));
        return new StocktakeDetailResponse(note,items.findByNoteIdOrderByItemId(id));
    }
    @Transactional
    public StocktakeNote create(StocktakeCreateRequest req) {
        List<UUID> lotIds=req.lotIds().stream().distinct().sorted().toList();
        if(lotIds.size()!=req.lotIds().size()) throw WarehouseSupport.error("Duplicate lotIds are not allowed");
        List<StockLot> found=lots.findByLotIdIn(lotIds); if(found.size()!=lotIds.size()) throw new AppException(WarehouseErrorCode.NOT_FOUND,"One or more lots were not found");
        List<UUID> productIds=found.stream().map(StockLot::getProductId).distinct().toList(); support.lockProducts(productIds);
        List<StockLot> locked=lots.lockByLotIds(lotIds);
        support.ensureLotsNotInDraftStocktake(lotIds);
        StocktakeNote n=new StocktakeNote(); n.setCode(WarehouseSupport.code("STK")); n.setStocktakeDate(LocalDateTime.now()); n.setCreatedBy(SecurityUtils.getCurrentUserId()); n.setHandleBy(req.handleBy()); n.setStatus(StocktakeStatus.DRAFT);
        StocktakeNote saved=notes.save(n); List<StocktakeItem> rows=new ArrayList<>();
        for(StockLot lot:locked) {
            if(lot.getStatus()==LotStatus.CANCELED) throw WarehouseSupport.error("Cancelled lots cannot be stocktaken");
            StocktakeItem row=new StocktakeItem();row.setNoteId(saved.getNoteId());row.setProductId(lot.getProductId());row.setLotId(lot.getLotId());row.setSystemQuantity(lot.getCurrentQuantity());row.setUnitCost(lot.getUnitCost());rows.add(row);
        }
        items.saveAll(rows);return saved;
    }
    @Transactional
    public List<StocktakeItem> count(UUID noteId,StocktakeCountRequest req) {
        StocktakeNote n=notes.lockById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake note not found"));
        if(n.getStatus()!=StocktakeStatus.DRAFT) throw new AppException(WarehouseErrorCode.CONFLICT,"Only DRAFT stocktakes accept counts");
        List<StocktakeItem> rows=items.findByNoteIdOrderByItemId(noteId); Map<UUID,StocktakeItem> rowMap=new HashMap<>();rows.forEach(r->rowMap.put(r.getLotId(),r));
        Set<UUID> seen=new HashSet<>();
        for(var count:req.items()) { if(!seen.add(count.lotId()))throw WarehouseSupport.error("Duplicate lot counts are not allowed"); StocktakeItem row=rowMap.get(count.lotId());if(row==null)throw WarehouseSupport.error("Lot is not part of this stocktake: "+count.lotId());row.setActualQuantity(count.actualQuantity());row.setReason(count.reason());row.setNote(count.note()); }
        return items.saveAll(rows);
    }
    @Transactional
    public StocktakeNote complete(UUID noteId) {
        StocktakeNote n=notes.lockById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake note not found"));
        if(n.getStatus()!=StocktakeStatus.DRAFT)throw new AppException(WarehouseErrorCode.CONFLICT,"Only DRAFT stocktakes can be completed");
        List<StocktakeItem> rows=items.findByNoteIdOrderByItemId(noteId);if(rows.isEmpty())throw WarehouseSupport.error("Stocktake must contain at least one lot");
        List<UUID> pids=rows.stream().map(StocktakeItem::getProductId).distinct().sorted().toList();Map<UUID,Product> products=new HashMap<>();support.lockProducts(pids).forEach(p->products.put(p.getProductId(),p));
        List<StockLot> lockedLots=lots.lockByLotIds(rows.stream().map(StocktakeItem::getLotId).toList());Map<UUID,StockLot> lotMap=new HashMap<>();lockedLots.forEach(l->lotMap.put(l.getLotId(),l));
        for(StocktakeItem row:rows) {
            if(row.getActualQuantity()==null)throw WarehouseSupport.error("Enter actual quantity for every lot before completing");
            StockLot lot=lotMap.get(row.getLotId()); if(lot==null)throw new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake lot no longer exists");
            if(lot.getCurrentQuantity().compareTo(row.getSystemQuantity())!=0)throw new AppException(WarehouseErrorCode.CONFLICT,"Lot changed during stocktake; cancel and create a new stocktake");
            BigDecimal diff=row.getActualQuantity().subtract(row.getSystemQuantity()); if(diff.signum()!=0&&(row.getReason()==null||row.getReason().isBlank()))throw WarehouseSupport.error("Reason is required when counted quantity differs");
            BigDecimal costAmount=WarehouseSupport.money(diff.multiply(row.getUnitCost()));if(costAmount.precision()-costAmount.scale()>13)throw WarehouseSupport.error("Stocktake cost amount exceeds the database precision limit");
            row.setDiffQuantity(diff);row.setCostAmount(costAmount);
            Product product=products.get(row.getProductId());if(product.getStockQuantity()==null)product.setStockQuantity(BigDecimal.ZERO);product.setStockQuantity(product.getStockQuantity().add(diff));
            lot.setCurrentQuantity(row.getActualQuantity());lot.setStatus(lot.getCurrentQuantity().signum()==0?LotStatus.EMPTY:lot.getExpiryDate()!=null&&lot.getExpiryDate().isBefore(LocalDate.now())?LotStatus.EXPIRED:LotStatus.STORAGE);
        }
        items.saveAll(rows);n.setStatus(StocktakeStatus.COMPLETED);n.setUpdatedAt(LocalDateTime.now());return notes.save(n);
    }
    @Transactional
    public StocktakeNote cancel(UUID noteId) {
        StocktakeNote n=notes.lockById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stocktake note not found"));
        if(n.getStatus()!=StocktakeStatus.DRAFT)throw new AppException(WarehouseErrorCode.CONFLICT,"Only DRAFT stocktakes can be cancelled");
        n.setStatus(StocktakeStatus.CANCELLED);n.setUpdatedAt(LocalDateTime.now());return notes.save(n);
    }
}
