package com.manhnv.capstonebakerai.shipment.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.shipment.dto.response.ShipmentResponse;
import com.manhnv.capstonebakerai.shipment.service.ShipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/orders/{orderId}/shipment")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
@Tag(name = "Vận chuyển")
public class StaffShipmentController {

    private final ShipmentService service;

    @Operation(summary = "Xem chi tiết vận chuyển của đơn hàng ")
    @GetMapping
    public ResponseEntity<ApiResponse<ShipmentResponse>> get(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByOrderId(orderId)));
    }
}
