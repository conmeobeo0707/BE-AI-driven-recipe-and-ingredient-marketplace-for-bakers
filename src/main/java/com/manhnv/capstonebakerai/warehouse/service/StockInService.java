package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
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
public class StockInService {
    private final StockInNoteRepository notes; private final StockInItemRepository items;
    private final StockLotRepository lots; private final SupplierRepository suppliers;
    private final WarehouseSupport support;
    public StockInService(StockInNoteRepository notes, StockInItemRepository items, StockLotRepository lots,
                          SupplierRepository suppliers, WarehouseSupport support) {
        this.notes=notes; this.items=items; this.lots=lots; this.suppliers=suppliers; this.support=support;
    }
    @Transactional(readOnly = true)
    public Page<StockInNote> list(NoteStatus status, Pageable pageable) { return notes.search(status, pageable); }
    @Transactional(readOnly = true)
    public StockInDetailResponse get(UUID id) {
        StockInNote note=notes.findById(id).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-in note not found"));
        return new StockInDetailResponse(note,items.findByNoteIdOrderByItemId(id));
    }
    @Transactional
    public StockInNote create(StockInCreateRequest req) {
        if (!suppliers.existsById(req.supplierId())) throw new AppException(WarehouseErrorCode.NOT_FOUND, "Supplier not found");
        var ids=req.items().stream().map(StockInItemRequest::productId).distinct().toList();
        Map<UUID,Product> productMap=new HashMap<>(); support.lockProducts(ids).forEach(p->productMap.put(p.getProductId(),p));
        BigDecimal total=BigDecimal.ZERO; List<StockInItem> pendingItems=new ArrayList<>();
        StockInNote n=new StockInNote(); n.setCode(WarehouseSupport.code("SIN")); n.setSupplierId(req.supplierId());
        n.setReceiptDate(req.receiptDate()==null?LocalDateTime.now():req.receiptDate()); n.setNote(req.note());
        for (var r:req.items()) {
            Product p=productMap.get(r.productId()); if(p==null)throw new AppException(WarehouseErrorCode.NOT_FOUND,"Product not found");
            if(p.isCombo())throw WarehouseSupport.error("Inventory movements are not supported for combo products");
            ProductUnit u=support.unit(r.productId(),r.unitId());
            BigDecimal line=WarehouseSupport.money(r.quantity().multiply(r.unitPrice()));
            if(line.precision()-line.scale()>13)throw WarehouseSupport.error("Stock-in line total exceeds the database precision limit");
            total=total.add(line);
            StockInItem i=new StockInItem(); i.setNoteId(n.getNoteId()); i.setProductId(p.getProductId()); i.setUnitId(u.getUnitId());
            i.setQuantity(r.quantity()); i.setConversionFactor(u.getConversionFactor()); i.setUnitPrice(r.unitPrice()); i.setTotalPrice(line); i.setExpiryDate(r.expiryDate());
            // Persist item rows after the note ID has been generated.
            pendingItems.add(i);
        }
        total=WarehouseSupport.money(total); BigDecimal paid=req.paidAmount()==null?BigDecimal.ZERO:WarehouseSupport.money(req.paidAmount());
        if(paid.compareTo(total)>0) throw WarehouseSupport.error("Paid amount cannot exceed total amount");
        n.setTotalAmount(total); n.setPaidAmount(paid); n.setDebtAmount(total.subtract(paid));
        n.setPaymentStatus(paid.signum()==0?PaymentStatus.UNPAID:paid.compareTo(total)==0?PaymentStatus.PAID:PaymentStatus.PARTIAL);
        n.setStatus(NoteStatus.PENDING);
        StockInNote saved=notes.save(n);
        pendingItems.forEach(i->i.setNoteId(saved.getNoteId())); items.saveAll(pendingItems);
        return saved;
    }
    @Transactional
    public StockInNote complete(UUID id) {
        StockInNote n=notes.lockById(id).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-in note not found"));
        if(n.getStatus()!=NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT,"Only PENDING stock-in notes can be completed");
        List<StockInItem> lines=items.findByNoteIdOrderByItemId(id);
        var ids=lines.stream().map(StockInItem::getProductId).distinct().toList();
        Map<UUID,Product> productMap=new HashMap<>(); support.lockProducts(ids).forEach(p->productMap.put(p.getProductId(),p));
        for(StockInItem i:lines) {
            BigDecimal baseQty=i.getQuantity().multiply(i.getConversionFactor()).setScale(2, java.math.RoundingMode.HALF_UP);
            if(baseQty.signum()<=0||baseQty.precision()-baseQty.scale()>8)throw WarehouseSupport.error("Converted stock quantity exceeds the database precision limit");
            BigDecimal cost=WarehouseSupport.money(i.getUnitPrice().divide(i.getConversionFactor(), 8, java.math.RoundingMode.HALF_UP));
            StockLot lot=new StockLot(); lot.setProductId(i.getProductId()); lot.setStockInNoteId(id); lot.setLotCode("LOT-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT));
            lot.setInitialQuantity(baseQty); lot.setCurrentQuantity(baseQty); lot.setUnitCost(cost); lot.setExpiryDate(i.getExpiryDate());
            lot.setStatus(i.getExpiryDate()!=null&&i.getExpiryDate().isBefore(LocalDate.now())?LotStatus.EXPIRED:LotStatus.STORAGE); lots.save(lot);
            Product p=productMap.get(i.getProductId()); if(p.getStockQuantity()==null)p.setStockQuantity(BigDecimal.ZERO); p.setStockQuantity(p.getStockQuantity().add(baseQty));
        }
        n.setStatus(NoteStatus.COMPLETED); n.setUpdatedAt(LocalDateTime.now()); return notes.save(n);
    }
    @Transactional
    public StockInNote cancel(UUID id) {
        StockInNote n=notes.lockById(id).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-in note not found"));
        if(n.getStatus()!=NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT,"Only PENDING stock-in notes can be cancelled");
        n.setStatus(NoteStatus.CANCELLED); n.setUpdatedAt(LocalDateTime.now()); return notes.save(n);
    }
}
