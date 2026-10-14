package com.manhnv.capstonebakerai.order.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.order.dto.request.CancelOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.StaffCreateOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.UpdateOrderStatusRequest;
import com.manhnv.capstonebakerai.order.dto.response.OrderResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderSummaryResponse;
import com.manhnv.capstonebakerai.order.dto.response.RevenueReportResponse;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.order.enums.RevenuePeriodType;
import com.manhnv.capstonebakerai.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
public class StaffOrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody StaffCreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.createByStaff(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(service.getAllForStaff(status, paymentMethod, pageable)));
    }

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> revenue(
            @RequestParam RevenuePeriodType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        return ResponseEntity.ok(ApiResponse.success(service.getRevenueReport(type, start, end)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getForStaff(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateStatus(id, request.status())));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.cancelByStaff(id, request)));
    }
}
