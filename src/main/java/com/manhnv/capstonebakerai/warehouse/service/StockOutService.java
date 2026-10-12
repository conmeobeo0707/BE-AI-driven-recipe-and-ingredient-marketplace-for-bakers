package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
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
public class StockOutService {
    private final StockOutNoteRepository notes; private final StockOutItemRepository items;
    private final StockLotRepository lots; private final WarehouseSupport support;
    public StockOutService(StockOutNoteRepository notes, StockOutItemRepository items, StockLotRepository lots, WarehouseSupport support) {
        this.notes=notes;this.items=items;this.lots=lots;this.support=support;
    }
    @Transactional(readOnly = true)
    public Page<StockOutNote> list(NoteStatus status, StockOutType type, Pageable pageable) { return notes.search(status,type,pageable); }
    @Transactional(readOnly = true)
    public StockOutDetailResponse get(UUID id) {
        StockOutNote note=notes.findById(id).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-out note not found"));
        return new StockOutDetailResponse(note,items.findByNoteIdOrderByItemId(id));
    }
    @Transactional
    public StockOutNote create(StockOutCreateRequest req) {
        List<UUID> ids=req.items().stream().map(StockOutItemRequest::productId).distinct().toList();
        Map<UUID,Product> lockedProducts=new HashMap<>();support.lockProducts(ids).forEach(p->lockedProducts.put(p.getProductId(),p));
        StockOutNote n=new StockOutNote(); n.setCode(WarehouseSupport.code("SOUT")); n.setType(req.type()); n.setReferenceOrderId(req.referenceOrderId()); n.setNote(req.note()); n.setStatus(NoteStatus.PENDING); n.setTotalCost(BigDecimal.ZERO);
        StockOutNote saved=notes.save(n); List<StockOutItem> pending=new ArrayList<>();
        for(var r:req.items()) {
            Product p=lockedProducts.get(r.productId());if(p==null)throw new AppException(WarehouseErrorCode.NOT_FOUND,"Product not found");if(p.isCombo())throw WarehouseSupport.error("Inventory movements are not supported for combo products"); ProductUnit u=support.unit(r.productId(),r.unitId());
            if(req.type()==StockOutType.SALE && r.lotId()!=null) throw WarehouseSupport.error("SALE items use FEFO and must not specify lotId");
            if(req.type()==StockOutType.DISPOSAL && r.lotId()==null) throw WarehouseSupport.error("DISPOSAL items require lotId");
            if(r.lotId()!=null) { StockLot lot=lots.findById(r.lotId()).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Lot not found")); if(!lot.getProductId().equals(p.getProductId())) throw WarehouseSupport.error("Lot does not belong to product"); }
            ProductUnit baseUnit=support.baseUnit(p.getProductId());
            StockOutItem item=new StockOutItem(); item.setNoteId(saved.getNoteId()); item.setProductId(p.getProductId()); item.setLotId(r.lotId()); item.setUnitId(baseUnit.getUnitId());
            BigDecimal baseQuantity=r.quantity().multiply(u.getConversionFactor()).setScale(2,java.math.RoundingMode.HALF_UP);
            if(baseQuantity.signum()<=0||baseQuantity.precision()-baseQuantity.scale()>8)throw WarehouseSupport.error("Converted stock quantity exceeds the database precision limit");
            item.setQuantity(baseQuantity); item.setUnitPrice(BigDecimal.ZERO); item.setTotalPrice(BigDecimal.ZERO); pending.add(item);
        }
        items.saveAll(pending); return saved;
    }
    @Transactional
    public StockOutNote complete(UUID id) {
        StockOutNote n=notes.lockById(id).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-out note not found"));
        if(n.getStatus()!=NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT,"Only PENDING stock-out notes can be completed");
        List<StockOutItem> requested=items.findByNoteIdOrderByItemId(id); List<UUID> productIds=requested.stream().map(StockOutItem::getProductId).distinct().toList();
        Map<UUID,Product> products=new HashMap<>(); support.lockProducts(productIds).forEach(p->products.put(p.getProductId(),p));
        items.deleteByNoteId(id); BigDecimal total=BigDecimal.ZERO; LocalDate today=LocalDate.now();
        for(StockOutItem request:requested) {
            List<StockLot> selected;
            if(n.getType()==StockOutType.SALE) selected=lots.findSaleableLots(request.getProductId(),LotStatus.STORAGE,today);
            else { support.ensureLotsNotInDraftStocktake(List.of(request.getLotId())); selected=lots.lockByLotIds(List.of(request.getLotId())); }
            if(selected.isEmpty()) throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK,"No eligible lot for product " + request.getProductId());
            // Lock all eligible lots in a stable order before allocating to avoid concurrent overselling.
            if(n.getType()==StockOutType.SALE) {
                selected=lots.lockByLotIds(selected.stream().map(StockLot::getLotId).toList());
                selected.sort(Comparator.comparing(StockLot::getExpiryDate,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(StockLot::getCreatedAt).thenComparing(StockLot::getLotId));
                selected.removeIf(lot -> support.isLotInDraftStocktake(lot.getLotId()));
            }
            BigDecimal left=request.getQuantity();
            for(StockLot lot:selected) {
                if(!lot.getProductId().equals(request.getProductId()) || lot.getCurrentQuantity().signum()<=0 ||
                        (n.getType()==StockOutType.SALE && (lot.getStatus()!=LotStatus.STORAGE || lot.getExpiryDate()!=null&&lot.getExpiryDate().isBefore(today))) ||
                        (n.getType()==StockOutType.DISPOSAL && (lot.getStatus()==LotStatus.EMPTY||lot.getStatus()==LotStatus.CANCELED))) continue;
                BigDecimal qty=left.min(lot.getCurrentQuantity()); if(qty.signum()<=0) continue;
                BigDecimal lineCost=WarehouseSupport.money(qty.multiply(lot.getUnitCost()));
                if(lineCost.precision()-lineCost.scale()>13)throw WarehouseSupport.error("Stock-out line cost exceeds the database precision limit");
                StockOutItem out=new StockOutItem(); out.setNoteId(id);out.setProductId(request.getProductId());out.setLotId(lot.getLotId());out.setUnitId(request.getUnitId());out.setQuantity(qty);out.setUnitPrice(lot.getUnitCost());out.setTotalPrice(lineCost);items.save(out);
                lot.setCurrentQuantity(lot.getCurrentQuantity().subtract(qty)); lot.setStatus(lot.getCurrentQuantity().signum()==0?LotStatus.EMPTY:lot.getExpiryDate()!=null&&lot.getExpiryDate().isBefore(today)?LotStatus.EXPIRED:LotStatus.STORAGE);
                left=left.subtract(qty); total=total.add(out.getTotalPrice()); if(left.signum()==0) break;
            }
            if(left.signum()>0) throw new AppException(WarehouseErrorCode.INSUFFICIENT_STOCK,"Insufficient stock for product " + request.getProductId());
            Product product=products.get(request.getProductId()); if(product.getStockQuantity()==null)product.setStockQuantity(BigDecimal.ZERO); if(product.getStockQuantity().compareTo(request.getQuantity())<0) throw new AppException(WarehouseErrorCode.CONFLICT,"Product stock does not match lot quantities");
            product.setStockQuantity(product.getStockQuantity().subtract(request.getQuantity()));
        }
        total=WarehouseSupport.money(total);if(total.precision()-total.scale()>13)throw WarehouseSupport.error("Stock-out total cost exceeds the database precision limit");
        n.setTotalCost(total);n.setStatus(NoteStatus.COMPLETED);n.setUpdatedAt(LocalDateTime.now());return notes.save(n);
    }
    @Transactional
    public StockOutNote cancel(UUID id) {
        StockOutNote n=notes.lockById(id).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-out note not found"));
        if(n.getStatus()!=NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT,"Only PENDING stock-out notes can be cancelled");
        n.setStatus(NoteStatus.CANCELLED);n.setUpdatedAt(LocalDateTime.now());return notes.save(n);
    }
}
