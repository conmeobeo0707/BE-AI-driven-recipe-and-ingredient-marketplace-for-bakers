package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.SupplierRequest;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierResponse;
import com.manhnv.capstonebakerai.warehouse.entity.StockInNote;
import com.manhnv.capstonebakerai.warehouse.enums.NoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface SupplierService {
    SupplierResponse create(SupplierRequest request);
    SupplierResponse update(UUID supplierId, SupplierRequest request);
    SupplierResponse get(UUID supplierId);
    Page<SupplierResponse> list(String keyword, Pageable pageable);
    Page<StockInNote> getStockInHistory(UUID supplierId, NoteStatus status, Pageable pageable);
    void delete(UUID supplierId);
}
