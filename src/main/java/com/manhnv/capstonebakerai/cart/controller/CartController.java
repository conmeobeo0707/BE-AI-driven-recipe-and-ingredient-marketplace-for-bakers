package com.manhnv.capstonebakerai.cart.controller;

import com.manhnv.capstonebakerai.cart.dto.request.AddCartItemRequest;
import com.manhnv.capstonebakerai.cart.dto.request.UpdateCartItemQuantityRequest;
import com.manhnv.capstonebakerai.cart.dto.response.CartResponse;
import com.manhnv.capstonebakerai.cart.service.CartService;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
@Tag(name = "Giỏ hàng", description = "Các API thao tác với giỏ hàng của customer")
public class CartController {

    private final CartService service;

    @Operation(
            summary = "Lấy cart cho customer",
            description = "Trả về cart của customer, nếu chưa có thì hệ thống tự tạo mới."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.getMyCart(userId)));
    }

    @Operation(summary = "Thêm item vào cart cho customer")
    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddCartItemRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.addItem(userId, request)));
    }

    @Operation(summary = "Update quantity của item trong cart")
    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
            ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.updateQuantity(userId, itemId, request)));
    }

    @Operation(summary = "Xóa item trong cart")
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable UUID itemId
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.removeItem(userId, itemId)));
    }
}
