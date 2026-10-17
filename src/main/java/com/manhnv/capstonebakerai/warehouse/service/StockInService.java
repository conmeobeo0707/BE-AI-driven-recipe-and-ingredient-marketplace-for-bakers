package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.product.dtos.InventoryProduct;
import com.manhnv.capstonebakerai.product.dtos.InventoryUnit;
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
    public Page<StockInNote> list(NoteStatus status, Pageable pageable) {
        return status==null?notes.findAll(pageable):notes.findByStatus(status,pageable);
    }
    @Transactional(readOnly = true)
    public StockInDetailResponse get(UUID noteId) {
        StockInNote stockInNote=notes.findById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-in note not found"));
        return new StockInDetailResponse(stockInNote,items.findByNoteIdOrderByItemId(noteId));
    }
    @Transactional
    public StockInNote create(StockInCreateRequest request) {
        if (!suppliers.existsById(request.supplierId())) throw new AppException(WarehouseErrorCode.NOT_FOUND, "Supplier not found");
        List<UUID> productIds=request.items().stream().map(StockInItemRequest::productId).distinct().toList();
        Map<UUID,InventoryProduct> productsById=new HashMap<>();
        support.lockProducts(productIds).forEach(product->productsById.put(product.productId(),product));
        BigDecimal totalAmount=BigDecimal.ZERO; List<StockInItem> pendingItems=new ArrayList<>();
        StockInNote stockInNote=new StockInNote(); stockInNote.setCode(WarehouseSupport.code("SIN")); stockInNote.setSupplierId(request.supplierId());
        stockInNote.setReceiptDate(request.receiptDate()==null?LocalDateTime.now():request.receiptDate()); stockInNote.setNote(request.note());
        for (StockInItemRequest itemRequest:request.items()) {
            InventoryProduct inventoryProduct=productsById.get(itemRequest.productId());
            if(inventoryProduct==null)throw new AppException(WarehouseErrorCode.NOT_FOUND,"Product not found");
            InventoryUnit inventoryUnit=support.unit(itemRequest.productId(),itemRequest.unitId());
            BigDecimal lineTotal=WarehouseSupport.money(itemRequest.quantity().multiply(itemRequest.unitPrice()));
            if(lineTotal.precision()-lineTotal.scale()>13)throw WarehouseSupport.error("Stock-in line total exceeds the database precision limit");
            totalAmount=totalAmount.add(lineTotal);
            StockInItem stockInItem=new StockInItem(); stockInItem.setProductId(inventoryProduct.productId()); stockInItem.setUnitId(inventoryUnit.unitId());
            stockInItem.setQuantity(itemRequest.quantity()); stockInItem.setConversionFactor(inventoryUnit.conversionFactor());
            stockInItem.setUnitPrice(itemRequest.unitPrice()); stockInItem.setTotalPrice(lineTotal); stockInItem.setExpiryDate(itemRequest.expiryDate());
            pendingItems.add(stockInItem);
        }
        totalAmount=WarehouseSupport.money(totalAmount); BigDecimal paidAmount=request.paidAmount()==null?BigDecimal.ZERO:WarehouseSupport.money(request.paidAmount());
        if(paidAmount.compareTo(totalAmount)>0) throw WarehouseSupport.error("Paid amount cannot exceed total amount");
        stockInNote.setTotalAmount(totalAmount); stockInNote.setPaidAmount(paidAmount); stockInNote.setDebtAmount(totalAmount.subtract(paidAmount));
        stockInNote.setPaymentStatus(paidAmount.signum()==0?PaymentStatus.UNPAID:paidAmount.compareTo(totalAmount)==0?PaymentStatus.PAID:PaymentStatus.PARTIAL);
        stockInNote.setStatus(NoteStatus.PENDING);
        StockInNote savedNote=notes.save(stockInNote);
        pendingItems.forEach(stockInItem->stockInItem.setNoteId(savedNote.getNoteId())); items.saveAll(pendingItems);
        return savedNote;
    }
    @Transactional
    public StockInNote complete(UUID noteId) {
        StockInNote stockInNote=notes.lockById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-in note not found"));
        if(stockInNote.getStatus()!=NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT,"Only PENDING stock-in notes can be completed");
        List<StockInItem> stockInItems=items.findByNoteIdOrderByItemId(noteId);
        List<UUID> productIds=stockInItems.stream().map(StockInItem::getProductId).distinct().toList();
        support.lockProducts(productIds);
        Map<UUID,BigDecimal> stockDeltas=new HashMap<>();
        for(StockInItem stockInItem:stockInItems) {
            BigDecimal baseQuantity=stockInItem.getQuantity().multiply(stockInItem.getConversionFactor()).setScale(2, java.math.RoundingMode.HALF_UP);
            if(baseQuantity.signum()<=0||baseQuantity.precision()-baseQuantity.scale()>8)throw WarehouseSupport.error("Converted stock quantity exceeds the database precision limit");
            BigDecimal baseUnitCost=WarehouseSupport.money(stockInItem.getUnitPrice().divide(stockInItem.getConversionFactor(), 8, java.math.RoundingMode.HALF_UP));
            StockLot stockLot=new StockLot(); stockLot.setProductId(stockInItem.getProductId()); stockLot.setStockInNoteId(noteId); stockLot.setLotCode("LOT-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT));
            stockLot.setInitialQuantity(baseQuantity); stockLot.setCurrentQuantity(baseQuantity); stockLot.setUnitCost(baseUnitCost); stockLot.setExpiryDate(stockInItem.getExpiryDate());
            stockLot.setStatus(stockInItem.getExpiryDate()!=null&&stockInItem.getExpiryDate().isBefore(LocalDate.now())?LotStatus.EXPIRED:LotStatus.STORAGE); lots.save(stockLot);
            stockDeltas.merge(stockInItem.getProductId(),baseQuantity,BigDecimal::add);
        }
        support.adjustStock(stockDeltas);
        stockInNote.setStatus(NoteStatus.COMPLETED); stockInNote.setUpdatedAt(LocalDateTime.now()); return notes.save(stockInNote);
    }
    @Transactional
    public StockInNote cancel(UUID noteId) {
        StockInNote stockInNote=notes.lockById(noteId).orElseThrow(()->new AppException(WarehouseErrorCode.NOT_FOUND,"Stock-in note not found"));
        if(stockInNote.getStatus()!=NoteStatus.PENDING) throw new AppException(WarehouseErrorCode.CONFLICT,"Only PENDING stock-in notes can be cancelled");
        stockInNote.setStatus(NoteStatus.CANCELLED); stockInNote.setUpdatedAt(LocalDateTime.now()); return notes.save(stockInNote);
    }
}
