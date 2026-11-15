package com.manhnv.capstonebakerai.cart.service;

import com.manhnv.capstonebakerai.cart.dto.request.AddCartItemRequest;
import com.manhnv.capstonebakerai.cart.dto.response.CartResponse;
import com.manhnv.capstonebakerai.cart.entity.Cart;
import com.manhnv.capstonebakerai.cart.entity.CartItem;
import com.manhnv.capstonebakerai.cart.mapper.CartMapper;
import com.manhnv.capstonebakerai.cart.repository.CartItemRepository;
import com.manhnv.capstonebakerai.cart.repository.CartRepository;
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.product.repository.ProductRepository;
import com.manhnv.capstonebakerai.product.repository.ProductUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceImplTest {

    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductUnitRepository productUnitRepository;
    @Mock CartMapper cartMapper;

    @InjectMocks CartServiceImpl cartService;

    private final UUID userId = UUID.randomUUID();
    private final UUID cartId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();

    private Cart cart;
    private Product product;
    private ProductUnit unit;

    @BeforeEach
    void setUp() {
        cart = mock(Cart.class);
        product = mock(Product.class);
        unit = mock(ProductUnit.class);
    }

    // UTCID01
    @Test
    void addItem_newItem_shouldCreateItemWithRequestedQuantity() {
        AddCartItemRequest request = new AddCartItemRequest(productId, unitId, new BigDecimal("2"));

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(cartId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(product.getProductId()).thenReturn(productId);
        when(unit.getProductId()).thenReturn(productId);
        when(unit.isSale()).thenReturn(true);
        when(unit.getUnitId()).thenReturn(unitId);
        when(cartItemRepository.findExisting(cartId, productId, unitId)).thenReturn(Optional.empty());
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of());

        CartResponse response = cartService.addItem(userId, request);
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("2");
        assertThat(response).isNotNull();
    }

    // UTCID02
    @Test
    void addItem_existingItem_shouldIncreaseQuantity() {
        AddCartItemRequest request = new AddCartItemRequest(productId, unitId, new BigDecimal("3"));
        CartItem existing = CartItem.builder().quantity(new BigDecimal("2")).build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(cartId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(product.getProductId()).thenReturn(productId);
        when(unit.getProductId()).thenReturn(productId);
        when(unit.isSale()).thenReturn(true);
        when(unit.getUnitId()).thenReturn(unitId);
        when(cartItemRepository.findExisting(cartId, productId, unitId)).thenReturn(Optional.of(existing));
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of());

        cartService.addItem(userId, request);

        assertThat(existing.getQuantity()).isEqualByComparingTo("5");
        verify(cartItemRepository).save(existing);
    }

    // UTCID03
    @Test
    void addItem_noCart_shouldCreateCartFirst() {
        AddCartItemRequest request = new AddCartItemRequest(productId, unitId, BigDecimal.ONE);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cart.getId()).thenReturn(cartId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(product.getProductId()).thenReturn(productId);
        when(unit.getProductId()).thenReturn(productId);
        when(unit.isSale()).thenReturn(true);
        when(unit.getUnitId()).thenReturn(unitId);
        when(cartItemRepository.findExisting(cartId, productId, unitId)).thenReturn(Optional.empty());
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of());

        cartService.addItem(userId, request);

        verify(cartRepository).save(any(Cart.class));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    // UTCID04
    @Test
    void addItem_productNotFound_shouldThrow() {
        AddCartItemRequest request = new AddCartItemRequest(productId, unitId, BigDecimal.ONE);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(userId, request))
                .isInstanceOf(AppException.class);
        verify(cartItemRepository, never()).save(any());
    }

    // UTCID05
    @Test
    void addItem_unitNotFound_shouldThrow() {
        AddCartItemRequest request = new AddCartItemRequest(productId, unitId, BigDecimal.ONE);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productUnitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(userId, request))
                .isInstanceOf(AppException.class);
        verify(cartItemRepository, never()).save(any());
    }

    // UTCID06
    @Test
    void addItem_unitBelongsToOtherProduct_shouldThrow() {
        AddCartItemRequest request = new AddCartItemRequest(productId, unitId, BigDecimal.ONE);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(product.getProductId()).thenReturn(productId);
        when(unit.getProductId()).thenReturn(UUID.randomUUID());

        assertThatThrownBy(() -> cartService.addItem(userId, request))
                .isInstanceOf(AppException.class);
        verify(cartItemRepository, never()).save(any());
    }

    // UTCID07
    @Test
    void addItem_unitNotForSale_shouldThrow() {
        AddCartItemRequest request = new AddCartItemRequest(productId, unitId, BigDecimal.ONE);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productUnitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(product.getProductId()).thenReturn(productId);
        when(unit.getProductId()).thenReturn(productId);
        when(unit.isSale()).thenReturn(false);

        assertThatThrownBy(() -> cartService.addItem(userId, request))
                .isInstanceOf(AppException.class);
        verify(cartItemRepository, never()).save(any());
    }

}
