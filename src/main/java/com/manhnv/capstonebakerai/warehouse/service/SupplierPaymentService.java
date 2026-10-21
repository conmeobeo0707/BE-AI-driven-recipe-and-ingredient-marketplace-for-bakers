package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentCreateRequest;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentDetailResponse;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SupplierPaymentService {
    SupplierPaymentDetailResponse create(UUID supplierId, SupplierPaymentCreateRequest request);
    Page<SupplierPaymentResponse> list(UUID supplierId, Pageable pageable);
    SupplierPaymentDetailResponse get(UUID supplierId, UUID paymentId);
}
