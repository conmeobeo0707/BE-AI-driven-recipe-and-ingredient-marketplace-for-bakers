package com.manhnv.capstonebakerai.cart.service;

import com.manhnv.capstonebakerai.cart.dto.request.AddCartItemRequest;
import com.manhnv.capstonebakerai.cart.dto.request.UpdateCartItemQuantityRequest;
import com.manhnv.capstonebakerai.cart.dto.response.CartResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

public interface CartService {
    CartResponse getMyCart(UUID userId);
    CartResponse addItem(UUID userId, AddCartItemRequest request);
    CartResponse updateQuantity(UUID userId, UUID itemId, UpdateCartItemQuantityRequest request);
    CartResponse removeItem(UUID userId, UUID itemId);
}
