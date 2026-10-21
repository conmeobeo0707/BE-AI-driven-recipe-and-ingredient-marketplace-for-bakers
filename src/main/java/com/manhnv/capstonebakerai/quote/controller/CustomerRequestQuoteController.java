package com.manhnv.capstonebakerai.quote.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.quote.dto.request.RequestQuoteRequest;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteResponse;
import com.manhnv.capstonebakerai.quote.dto.response.RequestQuoteSummaryResponse;
import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;
import com.manhnv.capstonebakerai.quote.service.RequestQuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/request-quotes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Yêu cầu đặt sỉ của khách hàng")
public class CustomerRequestQuoteController {

    private final RequestQuoteService service;

    @Operation(summary = "API tạo yêu cầu đặt sỉ",
            description = "Truyền danh sách productUnitId và quantity. contactPhone frontend tự điền từ hồ sơ, customer được sửa")
    @PostMapping
    public ResponseEntity<ApiResponse<RequestQuoteResponse>> create(@Valid @RequestBody RequestQuoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.create(SecurityUtils.getCurrentUserId(), request)));
    }

    @Operation(summary = "API lấy các yêu cầu đặt sỉ của customer",
            description = "Có thể lọc theo status (cho phép null), phân trang (mặc định page 0, size 20), sort theo createdAt")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<RequestQuoteSummaryResponse>>> mine(
            @RequestParam(required = false) RequestQuoteStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                service.getMine(SecurityUtils.getCurrentUserId(), status, pageable)));
    }

    @Operation(summary = "API lấy chi tiết yêu cầu đặt sỉ của customer")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestQuoteResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getMine(SecurityUtils.getCurrentUserId(), id)));
    }

    @Operation(summary = "API sửa yêu cầu đặt sỉ", description = "Chỉ sửa được khi đang PENDING")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestQuoteResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody RequestQuoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.updateMine(SecurityUtils.getCurrentUserId(), id, request)));
    }

    @Operation(summary = "API hủy yêu cầu đặt sỉ", description = "Hủy được khi đang PENDING hoặc IN_PROGRESS")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<RequestQuoteResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.cancelMine(SecurityUtils.getCurrentUserId(), id)));
    }
}
