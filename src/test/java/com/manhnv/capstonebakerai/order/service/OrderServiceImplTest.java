package com.manhnv.capstonebakerai.order.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.loyalty.service.LoyaltyService;
import com.manhnv.capstonebakerai.order.dto.request.*;
import com.manhnv.capstonebakerai.order.dto.response.*;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.enums.OrderHistoryAction;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.order.event.OrderCreatedEvent;
import com.manhnv.capstonebakerai.order.event.OrderStatusChangedEvent;
import com.manhnv.capstonebakerai.order.exception.OrderErrorCode;
import com.manhnv.capstonebakerai.order.mapper.OrderMapper;
import com.manhnv.capstonebakerai.order.repository.OrderRepository;
import com.manhnv.capstonebakerai.payment.service.PaymentService;
import com.manhnv.capstonebakerai.product.entities.Product;
import com.manhnv.capstonebakerai.product.entities.ProductUnit;
import com.manhnv.capstonebakerai.product.entities.UnitStatus;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import com.manhnv.capstonebakerai.product.repository.ProductRepository;
import com.manhnv.capstonebakerai.product.repository.ProductUnitRepository;
import com.manhnv.capstonebakerai.shipment.dto.response.ShippingRateResponse;
import com.manhnv.capstonebakerai.shipment.exception.ShipmentErrorCode;
import com.manhnv.capstonebakerai.shipment.service.ShipmentService;
import com.manhnv.capstonebakerai.shipment.service.ShippingRateService;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.service.UserSummaryService;
import com.manhnv.capstonebakerai.voucher.dto.request.ValidateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherPreviewResponse;
import com.manhnv.capstonebakerai.voucher.service.VoucherService;
import com.manhnv.capstonebakerai.warehouse.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    ProductUnitRepository productUnitRepository;
    @Mock
    OrderMapper mapper;
    @Mock
    ShipmentService shipmentService;
    @Mock
    ShippingRateService shippingRateService;
    @Mock
    PaymentService paymentService;
    @Mock
    VoucherService voucherService;
    @Mock OrderHistoryService historyService;
    @Mock
    InventoryService inventoryService;
    @Mock
    LoyaltyService loyaltyService;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    UserSummaryService userSummaryService;
    @InjectMocks
    OrderServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID UNIT_ID = UUID.randomUUID();
    private static final UUID VOUCHER_ID = UUID.randomUUID();
    private static final String CODE = "OD-ABC123";
    private static final String PROVINCE = "79";
    private static final String DISTRICT = "760";
    private static final String WARD = "26734";
    private static final String RATE_ID = "RATE1";
    private static final BigDecimal PRICE_200K = new BigDecimal("200000");

    // ---------- builders ----------
    private Customer customer() {
        User user = User.builder().userId(USER_ID).build();
        return Customer.builder().id(CUSTOMER_ID).user(user).build();
    }

    private Customer otherCustomer() {
        User user = User.builder().userId(UUID.randomUUID()).build();
        return Customer.builder().id(UUID.randomUUID()).user(user).build();
    }

    private Order order(OrderStatus status) {
        return Order.builder().id(ORDER_ID).code(CODE).customer(customer()).createdBy(USER_ID)
                .status(status).paymentMethod(PaymentMethod.COD).build();
    }

    private Product product() {
        Product p = new Product();
        p.setProductId(PRODUCT_ID);
        p.setName("Bot mi");
        p.setSku("SKU-1");
        p.setStatus(ProductStatus.ACTIVE);
        p.setDiscountPercent(BigDecimal.ZERO);
        return p;
    }

    private ProductUnit unit() {
        ProductUnit u = new ProductUnit();
        u.setUnitId(UNIT_ID);
        u.setProductId(PRODUCT_ID);
        u.setUnitName("Goi");
        u.setSellingPrice(new BigDecimal("100000"));
        u.setWeightGram(500);
        u.setStatus(UnitStatus.ACTIVE);
        u.setSale(true);
        return u;
    }

    private static <E extends Enum<E>> E anyOtherThan(E value) {
        return Arrays.stream(value.getDeclaringClass().getEnumConstants())
                .filter(e -> e != value).findFirst().orElseThrow();
    }

    private CreateOrderRequest createRequest(PaymentMethod method, String voucherCode) {
        return new CreateOrderRequest(method, "Giao gio hanh chinh", "Nguyen Van A", "0901234567",
                PROVINCE, DISTRICT, WARD, "12 Le Loi", "12 Le Loi, Quan 1", RATE_ID, voucherCode,
                List.of(new OrderItemRequest(UNIT_ID, 2)));
    }

    private OrderResponse baseResponse() {
        return new OrderResponse(ORDER_ID, CODE, CUSTOMER_ID, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    // ---------- stubs ----------
    private void stubCustomer() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.of(customer()));
    }

    private void stubOrder(Order order) {
        when(orderRepository.findWithItemsById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    private void stubUnit(ProductUnit unit, Product product) {
        when(productUnitRepository.findAllById(List.of(UNIT_ID))).thenReturn(List.of(unit));
        when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(product));
    }

    private UserSummaryResponse stubEnrich() {
        UserSummaryResponse creator = mock(UserSummaryResponse.class);
        when(mapper.toResponse(any(Order.class))).thenReturn(baseResponse());
        when(userSummaryService.getSummary(USER_ID)).thenReturn(creator);
        return creator;
    }

    private void stubSaveAssignsId() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(ORDER_ID);
            return o;
        });
    }

    private void stubRate(int weight, BigDecimal cod, BigDecimal productsPrice, BigDecimal codFee) {
        ShippingRateResponse rate = mock(ShippingRateResponse.class);
        when(rate.id()).thenReturn(RATE_ID);
        when(rate.carrierName()).thenReturn("GHN");
        when(rate.service()).thenReturn("Standard");
        when(rate.totalFee()).thenReturn(new BigDecimal("25000"));
        when(rate.codFee()).thenReturn(codFee);
        when(shippingRateService.requireRate(PROVINCE, DISTRICT, WARD, weight, cod, productsPrice, RATE_ID))
                .thenReturn(rate);
    }

    private VoucherPreviewResponse voucherPreview(String discount) {
        VoucherPreviewResponse preview = mock(VoucherPreviewResponse.class);
        when(preview.discountAmount()).thenReturn(new BigDecimal(discount));
        return preview;
    }

    private Order captureSavedOrder() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        return captor.getValue();
    }

    // create for customer
    // UTCID01
    @Test
    void createForCustomer_codNoVoucher_shouldCreatePendingOrderAndDeductStock() {
        stubCustomer();
        stubUnit(unit(), product());
        stubRate(1000, PRICE_200K, PRICE_200K, new BigDecimal("5000"));
        stubSaveAssignsId();
        UserSummaryResponse creator = stubEnrich();

        OrderResponse result = service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null));

        Order saved = captureSavedOrder();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getCustomer().getId()).isEqualTo(CUSTOMER_ID);
        assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Bot mi");
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(saved.getItems().get(0).getUnitPrice()).isEqualByComparingTo("100000");
        assertThat(saved.getItems().get(0).getLineTotal()).isEqualByComparingTo("200000");
        assertThat(saved.getProductsPrice()).isEqualByComparingTo("200000");
        assertThat(saved.getShippingFee()).isEqualByComparingTo("30000");
        assertThat(saved.getTotal()).isEqualByComparingTo("230000");
        assertThat(saved.getShippingRateId()).isEqualTo(RATE_ID);
        assertThat(saved.getCarrierName()).isEqualTo("GHN");
        assertThat(saved.getShippingService()).isEqualTo("Standard");

        verify(historyService).record(ORDER_ID, OrderHistoryAction.CREATED, null, OrderStatus.PENDING, null);
        verify(paymentService).createPending(ORDER_ID, PaymentMethod.COD, saved.getTotal());
        verify(inventoryService).deductForOrder(eq(ORDER_ID), anyList());
        verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
        verifyNoInteractions(voucherService);
        assertThat(result.id()).isEqualTo(ORDER_ID);
        assertThat(result.createdBy()).isSameAs(creator);
    }

    // UTCID02
    @Test
    void createForCustomer_payos_shouldAwaitPaymentWithoutDeductingStock() {
        stubCustomer();
        stubUnit(unit(), product());
        stubRate(1000, BigDecimal.ZERO, PRICE_200K, new BigDecimal("5000"));
        stubSaveAssignsId();
        stubEnrich();

        service.createForCustomer(USER_ID, createRequest(PaymentMethod.PAYOS, null));

        Order saved = captureSavedOrder();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verify(historyService).record(ORDER_ID, OrderHistoryAction.CREATED, null, OrderStatus.AWAITING_PAYMENT, null);
        verify(paymentService).createPending(ORDER_ID, PaymentMethod.PAYOS, saved.getTotal());
        verifyNoInteractions(inventoryService, eventPublisher);
    }

    // UTCID03
    @Test
    void createForCustomer_withVoucher_shouldApplyAndConsumeVoucher() {
        stubCustomer();
        stubUnit(unit(), product());
        VoucherPreviewResponse preview = voucherPreview("20000");
        when(preview.voucherId()).thenReturn(VOUCHER_ID);
        when(preview.code()).thenReturn("SALE10");
        when(voucherService.calculate("SALE10", CUSTOMER_ID, PRICE_200K)).thenReturn(preview);
        stubRate(1000, PRICE_200K, PRICE_200K, new BigDecimal("5000"));
        stubSaveAssignsId();
        stubEnrich();

        service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, "SALE10"));

        Order saved = captureSavedOrder();
        assertThat(saved.getVoucherId()).isEqualTo(VOUCHER_ID);
        assertThat(saved.getVoucherCode()).isEqualTo("SALE10");
        assertThat(saved.getVoucherDiscountAmount()).isEqualByComparingTo("20000");
        assertThat(saved.getTotal()).isEqualByComparingTo("210000");   // 200000 - 20000 + 30000
        verify(voucherService).consume(eq(VOUCHER_ID), eq(CUSTOMER_ID), eq(ORDER_ID), any(BigDecimal.class));
    }

    // UTCID04
    @Test
    void createForCustomer_productDiscount_shouldReduceUnitPrice() {
        Product product = product();
        product.setDiscountPercent(new BigDecimal("10"));
        stubCustomer();
        stubUnit(unit(), product);
        stubRate(1000, new BigDecimal("180000"), new BigDecimal("180000"), new BigDecimal("5000"));
        stubSaveAssignsId();
        stubEnrich();

        service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null));

        Order saved = captureSavedOrder();
        assertThat(saved.getItems().get(0).getUnitPrice()).isEqualByComparingTo("90000");
        assertThat(saved.getProductsPrice()).isEqualByComparingTo("180000");
        assertThat(saved.getTotal()).isEqualByComparingTo("210000");   // 180000 + 30000
    }

    // UTCID05
    @Test
    void createForCustomer_nullCodFee_shouldUseTotalFeeOnly() {
        stubCustomer();
        stubUnit(unit(), product());
        stubRate(1000, PRICE_200K, PRICE_200K, null);
        stubSaveAssignsId();
        stubEnrich();

        service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null));

        Order saved = captureSavedOrder();
        assertThat(saved.getShippingFee()).isEqualByComparingTo("25000");
        assertThat(saved.getTotal()).isEqualByComparingTo("225000");
    }

    // UTCID06
    @Test
    void createForCustomer_customerNotFound_shouldThrowCustomerNotFound() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(orderRepository, productUnitRepository, shippingRateService);
    }

    // UTCID07
    @Test
    void createForCustomer_unitNotFound_shouldThrowUnitNotFound() {
        stubCustomer();   // productUnitRepository.findAllById không stub => trả list rỗng

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_UNIT_NOT_FOUND);

        verify(orderRepository, never()).save(any(Order.class));
    }

    // UTCID08
    @Test
    void createForCustomer_productNotFound_shouldThrowNotAvailable() {
        stubCustomer();
        when(productUnitRepository.findAllById(List.of(UNIT_ID))).thenReturn(List.of(unit()));
        // productRepository.findAllById không stub => trả list rỗng

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_NOT_AVAILABLE);

        verify(orderRepository, never()).save(any(Order.class));
    }

    // UTCID09
    @Test
    void createForCustomer_productInactive_shouldThrowNotAvailable() {
        Product product = product();
        product.setStatus(anyOtherThan(ProductStatus.ACTIVE));
        stubCustomer();
        stubUnit(unit(), product);

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_NOT_AVAILABLE);

        verify(orderRepository, never()).save(any(Order.class));
    }

    // UTCID10
    @Test
    void createForCustomer_unitInactive_shouldThrowNotAvailable() {
        ProductUnit unit = unit();
        unit.setStatus(anyOtherThan(UnitStatus.ACTIVE));
        stubCustomer();
        stubUnit(unit, product());

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_NOT_AVAILABLE);
    }

    // UTCID11
    @Test
    void createForCustomer_unitNotForSale_shouldThrowNotAvailable() {
        ProductUnit unit = unit();
        unit.setSale(false);
        stubCustomer();
        stubUnit(unit, product());

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_NOT_AVAILABLE);
    }

    // UTCID12
    @Test
    void createForCustomer_weightNull_shouldThrowWeightMissing() {
        ProductUnit unit = unit();
        unit.setWeightGram(null);
        stubCustomer();
        stubUnit(unit, product());

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_WEIGHT_MISSING);
    }

    // UTCID13
    @Test
    void createForCustomer_weightZero_shouldThrowWeightMissing() {
        ProductUnit unit = unit();
        unit.setWeightGram(0);
        stubCustomer();
        stubUnit(unit, product());

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_WEIGHT_MISSING);
    }

    // UTCID14
    @Test
    void createForCustomer_rateNotFound_shouldThrowAndNotSave() {
        stubCustomer();
        stubUnit(unit(), product());
        when(shippingRateService.requireRate(any(), any(), any(), anyInt(), any(), any(), any()))
                .thenThrow(new AppException(ShipmentErrorCode.SHIPPING_RATE_NOT_FOUND));

        assertThatThrownBy(() -> service.createForCustomer(USER_ID, createRequest(PaymentMethod.COD, null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ShipmentErrorCode.SHIPPING_RATE_NOT_FOUND);

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(paymentService, inventoryService, eventPublisher);
    }

    // get my order
    // UTCID01
    @Test
    void getMyOrders_hasOrders_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Order order = order(OrderStatus.PENDING);
        OrderSummaryResponse summary = new OrderSummaryResponse(ORDER_ID, CODE, CUSTOMER_ID,
                OrderStatus.PENDING, PaymentMethod.COD, PRICE_200K, "Nguyen Van A", null);
        stubCustomer();
        when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));
        when(mapper.toSummary(order)).thenReturn(summary);

        Page<OrderSummaryResponse> result = service.getMyOrders(USER_ID, OrderStatus.PENDING, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(summary);
    }

    // UTCID02
    @Test
    void getMyOrders_noOrders_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        stubCustomer();
        when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<OrderSummaryResponse> result = service.getMyOrders(USER_ID, null, pageable);

        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(mapper);
    }

    // UTCID03
    @Test
    void getMyOrders_customerNotFound_shouldThrowCustomerNotFound() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyOrders(USER_ID, null, PageRequest.of(0, 10)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(orderRepository);
    }

    // Get my order by id
    // UTCID01
    @Test
    void getMyOrder_ownedOrder_shouldReturnEnrichedResponse() {
        stubCustomer();
        stubOrder(order(OrderStatus.PENDING));
        UserSummaryResponse creator = stubEnrich();

        OrderResponse result = service.getMyOrder(USER_ID, ORDER_ID);

        assertThat(result.id()).isEqualTo(ORDER_ID);
        assertThat(result.code()).isEqualTo(CODE);
        assertThat(result.createdBy()).isSameAs(creator);
    }

    // UTCID02
    @Test
    void getMyOrder_customerNotFound_shouldThrowCustomerNotFound() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyOrder(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(orderRepository);
    }

    // UTCID03
    @Test
    void getMyOrder_orderNotFound_shouldThrowOrderNotFound() {
        stubCustomer();
        when(orderRepository.findWithItemsById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyOrder(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);
    }

    // UTCID04
    @Test
    void getMyOrder_orderOfOtherCustomer_shouldThrowOrderNotFound() {
        Order other = order(OrderStatus.PENDING);
        other.setCustomer(otherCustomer());
        stubCustomer();
        stubOrder(other);

        assertThatThrownBy(() -> service.getMyOrder(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

        verifyNoInteractions(mapper, userSummaryService);
    }

    // trackMyOrder
    // UTCID01
    @Test
    void trackMyOrder_ownedOrder_shouldReturnTracking() {
        Order order = order(OrderStatus.SHIPPING);
        OrderTrackingResponse tracking = new OrderTrackingResponse(ORDER_ID, CODE, OrderStatus.SHIPPING,
                PaymentMethod.COD, null, null, null, null);
        stubCustomer();
        stubOrder(order);
        when(mapper.toTracking(order)).thenReturn(tracking);

        OrderTrackingResponse result = service.trackMyOrder(USER_ID, ORDER_ID);

        assertThat(result).isSameAs(tracking);
    }

    // UTCID02
    @Test
    void trackMyOrder_orderOfOtherCustomer_shouldThrowOrderNotFound() {
        Order other = order(OrderStatus.SHIPPING);
        other.setCustomer(otherCustomer());
        stubCustomer();
        stubOrder(other);

        assertThatThrownBy(() -> service.trackMyOrder(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

        verifyNoInteractions(mapper);
    }

    // cancelByCustomer
    // UTCID01
    @Test
    void cancelByCustomer_pendingOrder_shouldCancelAndRestock() {
        Order order = order(OrderStatus.PENDING);
        stubCustomer();
        stubOrder(order);
        when(orderRepository.save(order)).thenReturn(order);
        stubEnrich();

        service.cancelByCustomer(USER_ID, ORDER_ID, new CancelOrderRequest("Doi y"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(order.getCancelReason()).isEqualTo("Doi y");
        verify(historyService).record(ORDER_ID, OrderHistoryAction.CANCELLED,
                OrderStatus.PENDING, OrderStatus.CANCELLED, "Doi y");
        verify(inventoryService).restockForOrder(ORDER_ID);
        verify(shipmentService, never()).cancelForOrder(any(UUID.class));
        verify(voucherService).refund(ORDER_ID);
        verify(paymentService).onOrderCancelled(order);
        verify(eventPublisher).publishEvent(
                new OrderStatusChangedEvent(ORDER_ID, CODE, USER_ID, OrderStatus.CANCELLED, true));
        verify(orderRepository).save(order);
    }

    // UTCID02
    @Test
    void cancelByCustomer_confirmedOrder_shouldAlsoCancelShipment() {
        Order order = order(OrderStatus.CONFIRMED);
        stubCustomer();
        stubOrder(order);
        when(orderRepository.save(order)).thenReturn(order);
        stubEnrich();

        service.cancelByCustomer(USER_ID, ORDER_ID, new CancelOrderRequest("Doi y"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryService).restockForOrder(ORDER_ID);
        verify(shipmentService).cancelForOrder(ORDER_ID);
        verify(voucherService).refund(ORDER_ID);
        verify(paymentService).onOrderCancelled(order);
    }

    // UTCID03
    @Test
    void cancelByCustomer_awaitingPayment_shouldNotRestockOrCancelShipment() {
        Order order = order(OrderStatus.AWAITING_PAYMENT);
        stubCustomer();
        stubOrder(order);
        when(orderRepository.save(order)).thenReturn(order);
        stubEnrich();

        service.cancelByCustomer(USER_ID, ORDER_ID, new CancelOrderRequest("Khong mua nua"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verifyNoInteractions(inventoryService, shipmentService);
        verify(voucherService).refund(ORDER_ID);
        verify(paymentService).onOrderCancelled(order);
    }

    // UTCID04
    @Test
    void cancelByCustomer_nullRequest_shouldCancelWithoutReason() {
        Order order = order(OrderStatus.PENDING);
        stubCustomer();
        stubOrder(order);
        when(orderRepository.save(order)).thenReturn(order);
        stubEnrich();

        service.cancelByCustomer(USER_ID, ORDER_ID, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isNull();
        verify(historyService).record(ORDER_ID, OrderHistoryAction.CANCELLED,
                OrderStatus.PENDING, OrderStatus.CANCELLED, null);
    }

    // UTCID05
    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"READY_TO_SHIP", "SHIPPING", "DELIVERED", "CANCELLED"})
    void cancelByCustomer_notCancellableStatus_shouldThrowCannotCancel(OrderStatus status) {
        Order order = order(status);
        stubCustomer();
        stubOrder(order);

        assertThatThrownBy(() -> service.cancelByCustomer(USER_ID, ORDER_ID, new CancelOrderRequest("x")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_CANNOT_CANCEL);

        assertThat(order.getStatus()).isEqualTo(status);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(inventoryService, shipmentService, voucherService, paymentService, eventPublisher);
    }

    // UTCID06
    @Test
    void cancelByCustomer_orderOfOtherCustomer_shouldThrowOrderNotFound() {
        Order other = order(OrderStatus.PENDING);
        other.setCustomer(otherCustomer());
        stubCustomer();
        stubOrder(other);

        assertThatThrownBy(() -> service.cancelByCustomer(USER_ID, ORDER_ID, new CancelOrderRequest("x")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

        assertThat(other.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any(Order.class));
    }

    // Confirm received
    // UTCID01
    @Test
    void confirmReceived_firstTime_shouldSetTimestampAndRecordHistory() {
        Order order = order(OrderStatus.DELIVERED);
        stubCustomer();
        stubOrder(order);
        when(orderRepository.save(order)).thenReturn(order);
        stubEnrich();

        service.confirmReceived(USER_ID, ORDER_ID);

        assertThat(order.getReceivedConfirmedAt()).isNotNull();
        verify(historyService).record(ORDER_ID, OrderHistoryAction.RECEIVED_CONFIRMED,
                OrderStatus.DELIVERED, OrderStatus.CONFIRMED, null);
        verify(orderRepository).save(order);
    }

    // UTCID02
    @Test
    void confirmReceived_alreadyConfirmed_shouldKeepOldTimestampWithoutHistory() {
        Order order = order(OrderStatus.DELIVERED);
        LocalDateTime earlier = LocalDateTime.of(2026, 11, 1, 10, 0);
        order.setReceivedConfirmedAt(earlier);
        stubCustomer();
        stubOrder(order);
        when(orderRepository.save(order)).thenReturn(order);
        stubEnrich();

        service.confirmReceived(USER_ID, ORDER_ID);

        assertThat(order.getReceivedConfirmedAt()).isEqualTo(earlier);
        verifyNoInteractions(historyService);
        verify(orderRepository).save(order);
    }

    // UTCID03
    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"DELIVERED"})
    void confirmReceived_notDelivered_shouldThrowNotDelivered(OrderStatus status) {
        stubCustomer();
        stubOrder(order(status));

        assertThatThrownBy(() -> service.confirmReceived(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_NOT_DELIVERED);

        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(historyService);
    }

    // getMyOrderHistory
    // UTCID01
    @Test
    void getMyOrderHistory_ownedOrder_shouldReturnHistory() {
        OrderHistoryResponse entry = new OrderHistoryResponse(OrderHistoryAction.CREATED, null,
                OrderStatus.PENDING, null, null, null);
        List<OrderHistoryResponse> history = List.of(entry);
        stubCustomer();
        stubOrder(order(OrderStatus.PENDING));
        when(historyService.getForCustomer(ORDER_ID)).thenReturn(history);

        List<OrderHistoryResponse> result = service.getMyOrderHistory(USER_ID, ORDER_ID);

        assertThat(result).isSameAs(history);
    }

    // UTCID02
    @Test
    void getMyOrderHistory_orderOfOtherCustomer_shouldThrowOrderNotFound() {
        Order other = order(OrderStatus.PENDING);
        other.setCustomer(otherCustomer());
        stubCustomer();
        stubOrder(other);

        assertThatThrownBy(() -> service.getMyOrderHistory(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

        verifyNoInteractions(historyService);
    }

    // previewShippingRate
    // UTCID01
    @Test
    void previewShippingRates_cod_shouldUseProductsPriceAsCod() {
        List<ShippingRateResponse> rates = List.of(mock(ShippingRateResponse.class));
        stubUnit(unit(), product());
        when(shippingRateService.getRates(PROVINCE, DISTRICT, WARD, 1000, PRICE_200K, PRICE_200K)).thenReturn(rates);
        ShippingRatesRequest request = new ShippingRatesRequest(PaymentMethod.COD, PROVINCE, DISTRICT, WARD,
                List.of(new OrderItemRequest(UNIT_ID, 2)));

        List<ShippingRateResponse> result = service.previewShippingRates(request);

        assertThat(result).isSameAs(rates);
    }

    // UTCID02
    @Test
    void previewShippingRates_payos_shouldUseZeroCod() {
        List<ShippingRateResponse> rates = List.of(mock(ShippingRateResponse.class));
        stubUnit(unit(), product());
        when(shippingRateService.getRates(PROVINCE, DISTRICT, WARD, 1000, BigDecimal.ZERO, PRICE_200K)).thenReturn(rates);
        ShippingRatesRequest request = new ShippingRatesRequest(PaymentMethod.PAYOS, PROVINCE, DISTRICT, WARD,
                List.of(new OrderItemRequest(UNIT_ID, 2)));

        List<ShippingRateResponse> result = service.previewShippingRates(request);

        assertThat(result).isSameAs(rates);
    }

    // UTCID03
    @Test
    void previewShippingRates_sameUnitTwoLines_shouldSumWeightAndPrice() {
        List<ShippingRateResponse> rates = List.of(mock(ShippingRateResponse.class));
        stubUnit(unit(), product());
        // 500g x (1 + 3) = 2000g, 100000 x 4 = 400000
        when(shippingRateService.getRates(PROVINCE, DISTRICT, WARD, 2000, new BigDecimal("400000"),
                new BigDecimal("400000"))).thenReturn(rates);
        ShippingRatesRequest request = new ShippingRatesRequest(PaymentMethod.COD, PROVINCE, DISTRICT, WARD,
                List.of(new OrderItemRequest(UNIT_ID, 1), new OrderItemRequest(UNIT_ID, 3)));

        List<ShippingRateResponse> result = service.previewShippingRates(request);

        assertThat(result).isSameAs(rates);
    }

    // UTCID04
    @Test
    void previewShippingRates_unitNotFound_shouldThrowAndSkipGoship() {
        ShippingRatesRequest request = new ShippingRatesRequest(PaymentMethod.COD, PROVINCE, DISTRICT, WARD,
                List.of(new OrderItemRequest(UNIT_ID, 2)));

        assertThatThrownBy(() -> service.previewShippingRates(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_UNIT_NOT_FOUND);

        verifyNoInteractions(shippingRateService);
    }

    // UTCID05
    @Test
    void previewShippingRates_weightMissing_shouldThrowAndSkipGoship() {
        ProductUnit unit = unit();
        unit.setWeightGram(null);
        stubUnit(unit, product());
        ShippingRatesRequest request = new ShippingRatesRequest(PaymentMethod.COD, PROVINCE, DISTRICT, WARD,
                List.of(new OrderItemRequest(UNIT_ID, 2)));

        assertThatThrownBy(() -> service.previewShippingRates(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_WEIGHT_MISSING);

        verifyNoInteractions(shippingRateService);
    }

    // previewVoucher
    // UTCID01
    @Test
    void previewVoucher_validVoucher_shouldReturnPreview() {
        VoucherPreviewResponse preview = mock(VoucherPreviewResponse.class);
        stubCustomer();
        stubUnit(unit(), product());
        when(voucherService.calculate("SALE10", CUSTOMER_ID, PRICE_200K)).thenReturn(preview);

        VoucherPreviewResponse result = service.previewVoucher(USER_ID,
                new ValidateVoucherRequest("SALE10", List.of(new OrderItemRequest(UNIT_ID, 2))));

        assertThat(result).isSameAs(preview);
    }

    // UTCID02
    @Test
    void previewVoucher_customerNotFound_shouldThrowCustomerNotFound() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.previewVoucher(USER_ID,
                new ValidateVoucherRequest("SALE10", List.of(new OrderItemRequest(UNIT_ID, 2)))))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(voucherService, productUnitRepository);
    }

    // UTCID03
    @Test
    void previewVoucher_unitNotFound_shouldThrowUnitNotFound() {
        stubCustomer();

        assertThatThrownBy(() -> service.previewVoucher(USER_ID,
                new ValidateVoucherRequest("SALE10", List.of(new OrderItemRequest(UNIT_ID, 2)))))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.PRODUCT_UNIT_NOT_FOUND);

        verifyNoInteractions(voucherService);
    }

    // previewExpectedPoints
    // UTCID01
    @Test
    void previewExpectedPoints_noVoucher_shouldUseFullProductsPrice() {
        stubCustomer();
        stubUnit(unit(), product());
        when(loyaltyService.expectedOrderPoints(CUSTOMER_ID, PRICE_200K)).thenReturn(20);

        ExpectedPointsResponse result = service.previewExpectedPoints(USER_ID,
                new PreviewPointsRequest(List.of(new OrderItemRequest(UNIT_ID, 2)), null));

        assertThat(result.expectedPoints()).isEqualTo(20);
        assertThat(result.pointBase()).isEqualByComparingTo("200000");
        verifyNoInteractions(voucherService);
    }

    // UTCID02
    @Test
    void previewExpectedPoints_withVoucher_shouldSubtractDiscount() {
        VoucherPreviewResponse preview = voucherPreview("20000");
        stubCustomer();
        stubUnit(unit(), product());
        when(voucherService.calculate("SALE10", CUSTOMER_ID, PRICE_200K)).thenReturn(preview);
        when(loyaltyService.expectedOrderPoints(CUSTOMER_ID, new BigDecimal("180000"))).thenReturn(18);

        ExpectedPointsResponse result = service.previewExpectedPoints(USER_ID,
                new PreviewPointsRequest(List.of(new OrderItemRequest(UNIT_ID, 2)), "SALE10"));

        assertThat(result.expectedPoints()).isEqualTo(18);
        assertThat(result.pointBase()).isEqualByComparingTo("180000");
    }

    // UTCID03
    @Test
    void previewExpectedPoints_blankVoucher_shouldIgnoreVoucher() {
        stubCustomer();
        stubUnit(unit(), product());
        when(loyaltyService.expectedOrderPoints(CUSTOMER_ID, PRICE_200K)).thenReturn(20);

        ExpectedPointsResponse result = service.previewExpectedPoints(USER_ID,
                new PreviewPointsRequest(List.of(new OrderItemRequest(UNIT_ID, 2)), "  "));

        assertThat(result.pointBase()).isEqualByComparingTo("200000");
        verifyNoInteractions(voucherService);
    }

    // UTCID04
    @Test
    void previewExpectedPoints_discountExceedsPrice_shouldClampBaseToZero() {
        VoucherPreviewResponse preview = voucherPreview("300000");
        stubCustomer();
        stubUnit(unit(), product());
        when(voucherService.calculate("BIG", CUSTOMER_ID, PRICE_200K)).thenReturn(preview);
        when(loyaltyService.expectedOrderPoints(CUSTOMER_ID, BigDecimal.ZERO)).thenReturn(0);

        ExpectedPointsResponse result = service.previewExpectedPoints(USER_ID,
                new PreviewPointsRequest(List.of(new OrderItemRequest(UNIT_ID, 2)), "BIG"));

        assertThat(result.expectedPoints()).isZero();
        assertThat(result.pointBase()).isEqualByComparingTo("0");
    }

    // UTCID05
    @Test
    void previewExpectedPoints_customerNotFound_shouldThrowCustomerNotFound() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.previewExpectedPoints(USER_ID,
                new PreviewPointsRequest(List.of(new OrderItemRequest(UNIT_ID, 2)), null)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(loyaltyService, voucherService);
    }
}