package com.manhnv.capstonebakerai.warehouse.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentCreateRequest;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentDetailResponse;
import com.manhnv.capstonebakerai.warehouse.dto.SupplierPaymentResponse;
import com.manhnv.capstonebakerai.warehouse.service.SupplierPaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers/{supplierId}/payments")
public class SupplierPaymentController {
    private final SupplierPaymentService service;

    public SupplierPaymentController(SupplierPaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierPaymentDetailResponse> create(@PathVariable UUID supplierId,
            @Valid @RequestBody SupplierPaymentCreateRequest request) {
        return ApiResponse.success(service.create(supplierId, request));
    }

    @GetMapping
    public ApiResponse<Page<SupplierPaymentResponse>> list(@PathVariable UUID supplierId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(service.list(supplierId, pageable));
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<SupplierPaymentDetailResponse> get(@PathVariable UUID supplierId,
            @PathVariable UUID paymentId) {
        return ApiResponse.success(service.get(supplierId, paymentId));
    }
}
