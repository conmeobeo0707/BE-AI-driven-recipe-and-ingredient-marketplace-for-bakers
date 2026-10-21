package com.manhnv.capstonebakerai.warehouse.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.warehouse.dto.StockReservationCreateRequest;
import com.manhnv.capstonebakerai.warehouse.dto.StockReservationResponse;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutNote;
import com.manhnv.capstonebakerai.warehouse.service.StockReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouse/reservations")
public class StockReservationController {
    private final StockReservationService reservations;

    public StockReservationController(StockReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StockReservationResponse> reserve(@Valid @RequestBody StockReservationCreateRequest request) {
        return ApiResponse.success(reservations.reserve(request));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<StockReservationResponse> getByOrder(@PathVariable UUID orderId) {
        return ApiResponse.success(reservations.getByOrderId(orderId));
    }

    @PostMapping("/order/{orderId}/release")
    public ApiResponse<StockReservationResponse> release(@PathVariable UUID orderId) {
        return ApiResponse.success(reservations.release(orderId));
    }

    @PostMapping("/order/{orderId}/consume")
    public ApiResponse<StockOutNote> consume(@PathVariable UUID orderId) {
        return ApiResponse.success(reservations.consume(orderId));
    }
}
