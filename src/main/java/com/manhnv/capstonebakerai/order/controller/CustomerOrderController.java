package com.manhnv.capstonebakerai.order.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.order.dto.request.CancelOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.CreateOrderRequest;
import com.manhnv.capstonebakerai.order.dto.response.OrderResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderSummaryResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderTrackingResponse;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.service.OrderService;
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
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {

    private final OrderService service;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.createForCustomer(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> myOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                service.getMyOrders(SecurityUtils.getCurrentUserId(), status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getMyOrder(SecurityUtils.getCurrentUserId(), id)));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> tracking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.trackMyOrder(SecurityUtils.getCurrentUserId(), id)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.cancelByCustomer(SecurityUtils.getCurrentUserId(), id, request)));
    }

    @PatchMapping("/{id}/confirm-received")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmReceived(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.confirmReceived(SecurityUtils.getCurrentUserId(), id)));
    }
}
