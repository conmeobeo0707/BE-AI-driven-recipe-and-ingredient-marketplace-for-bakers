package com.manhnv.capstonebakerai.cart.controller;

import com.manhnv.capstonebakerai.cart.dto.request.AddCartItemRequest;
import com.manhnv.capstonebakerai.cart.dto.request.UpdateCartItemQuantityRequest;
import com.manhnv.capstonebakerai.cart.dto.response.CartResponse;
import com.manhnv.capstonebakerai.cart.service.CartService;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart() {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.getMyCart(userId)));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddCartItemRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.addItem(userId, request)));
    }

    @PatchMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
            ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.updateQuantity(userId, itemId, request)));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER')")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable UUID itemId
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.removeItem(userId, itemId)));
    }
}
