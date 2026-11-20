package com.manhnv.capstonebakerai.cart.service;

import com.manhnv.capstonebakerai.cart.dto.request.AddCartItemRequest;
import com.manhnv.capstonebakerai.cart.dto.request.UpdateCartItemQuantityRequest;
import com.manhnv.capstonebakerai.cart.dto.response.CartItemResponse;
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

    // View cart

    // UTCID01
    @Test
    void getMyCart_cartWithItems_shouldReturnItemsAndTotal() {
        // Arrange
        CartItem item1 = mock(CartItem.class);
        CartItem item2 = mock(CartItem.class);
        CartItemResponse response1 = mock(CartItemResponse.class);
        CartItemResponse response2 = mock(CartItemResponse.class);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(cartId);
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of(item1, item2));
        when(cartMapper.toItemResponse(item1)).thenReturn(response1);
        when(cartMapper.toItemResponse(item2)).thenReturn(response2);
        when(response1.subtotal()).thenReturn(new BigDecimal("50000"));
        when(response2.subtotal()).thenReturn(new BigDecimal("100000"));

        // Act
        CartResponse response = cartService.getMyCart(userId);

        // Assert
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo("150000");
    }

    // UTCID02
    @Test
    void getMyCart_emptyCart_shouldReturnEmptyResponse() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(cartId);
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of());

        // Act
        CartResponse response = cartService.getMyCart(userId);

        assertThat(response.items()).isEmpty();
        assertThat(response.totalItems()).isZero();
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // UTCID03
    void getMyCart_noCart_shouldCreateNewCart() {
        // Arrange
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cart.getId()).thenReturn(cartId);
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of());

        // Act
        CartResponse response = cartService.getMyCart(userId);

        // Assert
        verify(cartRepository).save(any(Cart.class));
        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // UTCID04
    @Test
    void getMyCart_singleItem_shouldReturnOneItem() {
        // Arrange
        CartItem item = mock(CartItem.class);
        CartItemResponse itemResponse = mock(CartItemResponse.class);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cart.getId()).thenReturn(cartId);
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of(item));
        when(cartMapper.toItemResponse(item)).thenReturn(itemResponse);
        when(itemResponse.subtotal()).thenReturn(new BigDecimal("75000"));

        // Act
        CartResponse response = cartService.getMyCart(userId);

        // Assert
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.totalAmount()).isEqualByComparingTo("75000");
    }

    // Update cart quantity

    // UTCID01
    @Test
    void updateQuantity_validItem_shouldUpdateQuantity() {
        // Arrange
        UUID itemId = UUID.randomUUID();
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(new BigDecimal("5"));
        CartItem item = CartItem.builder()
                .cart(cart)
                .quantity(new BigDecimal("2"))
                .build();

        when(cartItemRepository.findByIdAndCartUserId(itemId, userId)).thenReturn(Optional.of(item));
        when(cart.getId()).thenReturn(cartId);
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of());

        // Act
        CartResponse response = cartService.updateQuantity(userId, itemId, request);

        // Assert
        assertThat(item.getQuantity()).isEqualByComparingTo("5");
        verify(cartItemRepository).save(item);
        assertThat(response).isNotNull();
    }

    // UTCID02
    @Test
    void updateQuantity_decreaseQuantity_shouldUpdateToNewValue() {
        // Arrange
        UUID itemId = UUID.randomUUID();
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(BigDecimal.ONE);
        CartItem item = CartItem.builder()
                .cart(cart)
                .quantity(new BigDecimal("4"))
                .build();

        when(cartItemRepository.findByIdAndCartUserId(itemId, userId)).thenReturn(Optional.of(item));
        when(cart.getId()).thenReturn(cartId);
        when(cartItemRepository.findAllByCartId(cartId)).thenReturn(List.of());

        // Act
        cartService.updateQuantity(userId, itemId, request);

        // Assert
        assertThat(item.getQuantity()).isEqualByComparingTo("1");
        verify(cartItemRepository).save(item);
    }

    // UTCID03
    @Test
    void updateQuantity_itemNotFound_shouldThrow() {
        // Arrange
        UUID itemId = UUID.randomUUID();
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(new BigDecimal("5"));

        when(cartItemRepository.findByIdAndCartUserId(itemId, userId)).thenReturn(Optional.empty());

        // Act - Assert
        assertThatThrownBy(() -> cartService.updateQuantity(userId, itemId, request))
                .isInstanceOf(AppException.class);
        verify(cartItemRepository, never()).save(any());
    }

    // UTCID04
    @Test
    void updateQuantity_itemBelongsToOtherUser_shouldThrow() {
        // Arrange
        UUID itemId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(new BigDecimal("5"));

        when(cartItemRepository.findByIdAndCartUserId(itemId, otherUserId)).thenReturn(Optional.empty());

        // Act - Assert
        assertThatThrownBy(() -> cartService.updateQuantity(otherUserId, itemId, request))
                .isInstanceOf(AppException.class);
        verify(cartItemRepository, never()).save(any());
    }

}
