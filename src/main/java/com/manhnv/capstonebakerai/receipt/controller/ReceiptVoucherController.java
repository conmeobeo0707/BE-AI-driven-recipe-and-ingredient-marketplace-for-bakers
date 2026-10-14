package com.manhnv.capstonebakerai.receipt.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.receipt.dto.request.CreateReceiptVoucherRequest;
import com.manhnv.capstonebakerai.receipt.dto.request.ReceiptCategoryRequest;
import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptCategoryResponse;
import com.manhnv.capstonebakerai.receipt.dto.response.ReceiptVoucherResponse;
import com.manhnv.capstonebakerai.receipt.service.ReceiptCategoryService;
import com.manhnv.capstonebakerai.receipt.service.ReceiptVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receipt-vouchers")
@RequiredArgsConstructor
public class ReceiptVoucherController {

    private final ReceiptVoucherService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ReceiptVoucherResponse>> create(@Valid @RequestBody CreateReceiptVoucherRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.create(userId, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ReceiptVoucherResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<ReceiptVoucherResponse>>> getAll(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = " 0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(service.getAll(categoryId, pageable)));
    }
}