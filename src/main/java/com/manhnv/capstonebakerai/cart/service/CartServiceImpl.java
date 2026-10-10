package com.manhnv.capstonebakerai.cart.service;

import com.manhnv.capstonebakerai.cart.dto.request.AddCartItemRequest;
import com.manhnv.capstonebakerai.cart.dto.request.UpdateCartItemQuantityRequest;
import com.manhnv.capstonebakerai.cart.dto.response.CartItemResponse;
import com.manhnv.capstonebakerai.cart.dto.response.CartResponse;
import com.manhnv.capstonebakerai.cart.entity.Cart;
import com.manhnv.capstonebakerai.cart.entity.CartItem;
import com.manhnv.capstonebakerai.cart.exception.CartErrorCode;
import com.manhnv.capstonebakerai.cart.mapper.CartMapper;
import com.manhnv.capstonebakerai.cart.repository.CartItemRepository;
import com.manhnv.capstonebakerai.cart.repository.CartRepository;
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.dtos.ProductResponse;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.product.repository.ProductRepository;
import com.manhnv.capstonebakerai.product.repository.ProductUnitRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartServiceImpl implements CartService{

    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    ProductRepository productRepository;
    ProductUnitRepository productUnitRepository;
    CartMapper cartMapper;

    @Override
    @Transactional
    public CartResponse getMyCart(UUID userId) {
        return buildResponse(getOrCreateCart(userId));
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new AppException(CartErrorCode.CART_PRODUCT_NOT_FOUND));

        ProductUnit unit = productUnitRepository.findById(request.uomId())
                .orElseThrow(() -> new AppException(CartErrorCode.CART_UNIT_NOT_FOUND));

        if (!unit.getProductId().equals(product.getProductId())
        || !unit.isSale()) {
            throw new AppException(CartErrorCode.CART_UNIT_NOT_AVAILABLE);
        }

        CartItem item = cartItemRepository
                .findExisting(cart.getId(), product.getProductId(), unit.getUnitId())
                .orElse(null);

        if (item != null) {
            item.setQuantity(item.getQuantity().add(request.quantity()));
        } else {
            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .uom(unit)
                    .build();
        }
        cartItemRepository.save(item);

        return buildResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateQuantity(UUID userId, UUID itemId, UpdateCartItemQuantityRequest request) {
        CartItem item = findItemOrThrow(userId, itemId);
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return buildResponse(item.getCart());
    }

    @Override
    @Transactional
    public CartResponse removeItem(UUID userId, UUID itemId) {
        CartItem item = findItemOrThrow(userId, itemId);
        Cart cart = item.getCart();
        cartItemRepository.delete(item);
        return buildResponse(cart);
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder()
                        .userId(userId)
                        .build())
                );
    }

    private CartItem findItemOrThrow(UUID userId, UUID itemId) {
        return cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new AppException(CartErrorCode.CART_ITEM_NOT_FOUND));
    }

    private CartResponse buildResponse(Cart cart) {
        List<CartItemResponse> items = cartItemRepository.findAllByCartId(cart.getId())
                .stream().map(cartMapper::toItemResponse).toList();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), items, items.size(), totalAmount);
    }
}
