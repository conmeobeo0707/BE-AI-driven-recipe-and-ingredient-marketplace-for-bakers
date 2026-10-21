package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierRequest;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierResponse;
import com.manhnv.capstonebakerai.warehouse.entity.Supplier;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;
import com.manhnv.capstonebakerai.warehouse.repository.StockInNoteRepository;
import com.manhnv.capstonebakerai.warehouse.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class SupplierServiceImpl implements SupplierService {
    private final SupplierRepository suppliers;
    private final StockInNoteRepository stockInNotes;

    public SupplierServiceImpl(SupplierRepository suppliers, StockInNoteRepository stockInNotes) {
        this.suppliers = suppliers;
        this.stockInNotes = stockInNotes;
    }

    @Override
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = new Supplier();
        apply(supplier, request);
        return response(suppliers.save(supplier));
    }

    @Override
    public SupplierResponse update(UUID supplierId, SupplierRequest request) {
        Supplier supplier = suppliers.findById(supplierId).orElseThrow(() -> notFound(supplierId));
        apply(supplier, request);
        return response(suppliers.save(supplier));
    }

    @Override @Transactional(readOnly = true)
    public SupplierResponse get(UUID supplierId) {
        return response(suppliers.findById(supplierId).orElseThrow(() -> notFound(supplierId)));
    }

    @Override @Transactional(readOnly = true)
    public Page<SupplierResponse> list(String keyword, Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Supplier> results = normalizedKeyword == null ? suppliers.findAll(pageable) : suppliers.search(normalizedKeyword, pageable);
        return results.map(this::response);
    }

    @Override
    public void delete(UUID supplierId) {
        Supplier supplier = suppliers.findById(supplierId).orElseThrow(() -> notFound(supplierId));
        if (stockInNotes.existsBySupplierId(supplierId)) throw new AppException(WarehouseErrorCode.CONFLICT, "Supplier is referenced by stock-in notes and cannot be deleted");
        suppliers.delete(supplier);
    }

    private void apply(Supplier supplier, SupplierRequest request) {
        supplier.setSupplierName(request.supplierName().trim());
        supplier.setPhoneNumber(clean(request.phoneNumber()));
        supplier.setEmail(clean(request.email()));
        supplier.setAddress(clean(request.address()));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private SupplierResponse response(Supplier supplier) {
        return new SupplierResponse(supplier.getSupplierId(), supplier.getSupplierName(), supplier.getPhoneNumber(), supplier.getEmail(), supplier.getAddress(), supplier.getCreatedAt());
    }

    private AppException notFound(UUID supplierId) { return new AppException(WarehouseErrorCode.NOT_FOUND, "Supplier not found: " + supplierId); }
}
