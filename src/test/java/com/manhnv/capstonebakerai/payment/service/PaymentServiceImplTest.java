package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.order.exception.OrderErrorCode;
import com.manhnv.capstonebakerai.order.repository.OrderRepository;
import com.manhnv.capstonebakerai.payment.dto.response.PayosLinkResponse;
import com.manhnv.capstonebakerai.payment.entity.Payment;
import com.manhnv.capstonebakerai.payment.enums.PaymentDirection;
import com.manhnv.capstonebakerai.payment.enums.PaymentStatus;
import com.manhnv.capstonebakerai.payment.enums.PaymentType;
import com.manhnv.capstonebakerai.payment.event.PaymentPaidEvent;
import com.manhnv.capstonebakerai.payment.exception.PaymentErrorCode;
import com.manhnv.capstonebakerai.payment.repository.PaymentRepository;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import com.manhnv.capstonebakerai.receipt.service.ReceiptVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    PaymentRepository paymentRepository;
    @Mock
    OrderRepository orderRepository;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    ReceiptVoucherService receiptVoucherService;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    PayOS payOS;
    @Mock
    Webhook webhook;
    @InjectMocks
    PaymentServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final long ORDER_CODE = 123456789L;
    private static final String EXTERNAL_ID = "123456789";
    private static final String OLD_URL = "https://pay.payos.vn/old";
    private static final String NEW_URL = "https://pay.payos.vn/new";
    private static final String RETURN_URL = "https://shop.vn/return";
    private static final String CANCEL_URL = "https://shop.vn/cancel";
    private static final String CANCEL_REASON = "Đơn hàng đã bị hủy";
    private static final BigDecimal AMOUNT = new BigDecimal("150000");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "returnUrl", RETURN_URL);
        ReflectionTestUtils.setField(service, "cancelUrl", CANCEL_URL);
    }

    // ---------- builders ----------
    private Customer customer(UUID id) {
        return Customer.builder().id(id).build();
    }

    private Order order(OrderStatus status) {
        return Order.builder().id(ORDER_ID).code("ORD-001").customer(customer(CUSTOMER_ID))
                .status(status).total(AMOUNT).build();
    }

    private Order orderForCancel() {
        return Order.builder().id(ORDER_ID).code("ORD-001").build();
    }

    private Payment payment(PaymentMethod method, PaymentStatus status) {
        return Payment.builder().id(PAYMENT_ID).orderId(ORDER_ID)
                .type(PaymentType.SALE).direction(PaymentDirection.IN)
                .method(method).status(status).amount(AMOUNT).build();
    }

    private Payment payosPaymentWithLink() {
        Payment p = payment(PaymentMethod.PAYOS, PaymentStatus.PENDING);
        p.setExternalId(EXTERNAL_ID);
        p.setCheckoutUrl(OLD_URL);
        return p;
    }

    // ---------- stubs ----------
    private void stubCustomer() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.of(customer(CUSTOMER_ID)));
    }

    private void stubOrder(Order order) {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    private void stubPaymentByOrder(Payment payment) {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));
    }

    private void stubValidContext(Order order, Payment payment) {
        stubCustomer();
        stubOrder(order);
        stubPaymentByOrder(payment);
    }

    private PaymentLink linkWithStatus(PaymentLinkStatus status) {
        PaymentLink link = mock(PaymentLink.class);
        when(link.getStatus()).thenReturn(status);
        return link;
    }

    private void stubGetLink(PaymentLinkStatus status) {
        PaymentLink link = linkWithStatus(status);
        when(payOS.paymentRequests().get(ORDER_CODE)).thenReturn(link);
    }

    private void stubGetLinkFails() {
        when(payOS.paymentRequests().get(ORDER_CODE)).thenThrow(new PayOSException("get failed"));
    }

    private void stubCreateLink(String url) {
        CreatePaymentLinkResponse res = mock(CreatePaymentLinkResponse.class);
        when(res.getCheckoutUrl()).thenReturn(url);
        when(payOS.paymentRequests().create(any(CreatePaymentLinkRequest.class))).thenReturn(res);
    }

    private void stubCancelFails() {
        when(payOS.paymentRequests().cancel(ORDER_CODE, CANCEL_REASON))
                .thenThrow(new PayOSException("cancel failed"));
    }

    private WebhookData webhookData(String code, long amount) {
        WebhookData data = mock(WebhookData.class);
        lenient().when(data.getOrderCode()).thenReturn(ORDER_CODE);
        lenient().when(data.getCode()).thenReturn(code);
        lenient().when(data.getAmount()).thenReturn(amount);
        lenient().when(data.getReference()).thenReturn("FT123");
        return data;
    }

    private void stubVerify(WebhookData data) throws Exception {
        when(payOS.webhooks().verify(webhook)).thenReturn(data);
    }

    private void stubPaymentByExternalId(Payment payment) {
        when(paymentRepository.findByExternalIdForUpdate(EXTERNAL_ID)).thenReturn(Optional.ofNullable(payment));
    }

    private void verifyMarkedPaid(Payment p, ReceiptCategoryType type, String bankRef, String note) {
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(p.getPaidAt()).isNotNull();
        assertThat(p.getBankReference()).isEqualTo(bankRef);
        assertThat(p.getNote()).isEqualTo(note);
        verify(paymentRepository).save(p);
        verify(receiptVoucherService).createFromPayment(PAYMENT_ID, AMOUNT, type, note);
    }

    private void verifyNoCreateLink() {
        verify(payOS.paymentRequests(), never()).create(any(CreatePaymentLinkRequest.class));
    }

    // Create pending
    // UTCID01
    @Test
    void createPending_shouldSavePendingSalePayment() {
        service.createPending(ORDER_ID, PaymentMethod.PAYOS, AMOUNT);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(PaymentType.SALE);
        assertThat(saved.getDirection()).isEqualTo(PaymentDirection.IN);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(saved.getMethod()).isEqualTo(PaymentMethod.PAYOS);
        assertThat(saved.getAmount()).isEqualByComparingTo("150000");
    }

    // Create payos link
    // UTCID01
    @Test
    void createPayosLink_customerNotFound_shouldThrowCustomerNotFound() {
        when(customerRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.CUSTOMER_NOT_FOUND);

        verifyNoInteractions(orderRepository, payOS);
    }

    // UTCID02
    @Test
    void createPayosLink_orderNotFound_shouldThrowOrderNotFound() {
        stubCustomer();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

        verifyNoInteractions(paymentRepository, payOS);
    }

    // UTCID03
    @Test
    void createPayosLink_orderOfOtherCustomer_shouldThrowOrderNotFound() {
        stubCustomer();
        Order other = order(OrderStatus.AWAITING_PAYMENT);
        other.setCustomer(customer(UUID.randomUUID()));
        stubOrder(other);

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_NOT_FOUND);

        verifyNoInteractions(paymentRepository, payOS);
    }

    // UTCID04
    @Test
    void createPayosLink_paymentNotFound_shouldThrowPaymentNotFound() {
        stubCustomer();
        stubOrder(order(OrderStatus.AWAITING_PAYMENT));
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);

        verifyNoInteractions(payOS);
    }

    // UTCID05
    @Test
    void createPayosLink_methodNotPayos_shouldThrowPaymentNotPayos() {
        stubValidContext(order(OrderStatus.AWAITING_PAYMENT), payment(PaymentMethod.COD, PaymentStatus.PENDING));

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.PAYMENT_NOT_PAYOS);

        verifyNoInteractions(payOS);
    }

    // UTCID06
    @Test
    void createPayosLink_paymentNotPending_shouldThrowPaymentNotPending() {
        stubValidContext(order(OrderStatus.AWAITING_PAYMENT), payment(PaymentMethod.PAYOS, PaymentStatus.PAID));

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.PAYMENT_NOT_PENDING);

        verifyNoInteractions(payOS);
    }

    // UTCID07
    @Test
    void createPayosLink_orderNotAwaitingPayment_shouldThrowInvalidTransition() {
        stubValidContext(order(OrderStatus.SHIPPING), payment(PaymentMethod.PAYOS, PaymentStatus.PENDING));

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION);

        verifyNoInteractions(payOS);
    }

    // UTCID08
    @Test
    void createPayosLink_noExistingLink_shouldCreateAndSaveNewLink() {
        Payment payment = payment(PaymentMethod.PAYOS, PaymentStatus.PENDING);
        stubValidContext(order(OrderStatus.AWAITING_PAYMENT), payment);
        stubCreateLink(NEW_URL);

        PayosLinkResponse result = service.createPayosLink(USER_ID, ORDER_ID);

        ArgumentCaptor<CreatePaymentLinkRequest> captor = ArgumentCaptor.forClass(CreatePaymentLinkRequest.class);
        verify(payOS.paymentRequests()).create(captor.capture());
        CreatePaymentLinkRequest req = captor.getValue();
        assertThat(req.getAmount()).isEqualTo(150000L);
        assertThat(req.getDescription()).isEqualTo("Thanh toán ORD-001");
        assertThat(req.getReturnUrl()).isEqualTo(RETURN_URL);
        assertThat(req.getCancelUrl()).isEqualTo(CANCEL_URL);

        assertThat(payment.getExternalId()).isEqualTo(String.valueOf(req.getOrderCode()));
        assertThat(payment.getCheckoutUrl()).isEqualTo(NEW_URL);
        assertThat(result).isEqualTo(new PayosLinkResponse(PAYMENT_ID, NEW_URL));
        verify(paymentRepository).save(payment);
    }

    // UTCID09
    @Test
    void createPayosLink_payosCreateFails_shouldThrowCreateLinkFailed() {
        Payment payment = payment(PaymentMethod.PAYOS, PaymentStatus.PENDING);
        stubValidContext(order(OrderStatus.AWAITING_PAYMENT), payment);
        when(payOS.paymentRequests().create(any(CreatePaymentLinkRequest.class)))
                .thenThrow(new PayOSException("create failed"));

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.PAYOS_CREATE_LINK_FAILED);

        assertThat(payment.getCheckoutUrl()).isNull();
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // UTCID10
    @Test
    void createPayosLink_getOldLinkFails_shouldThrowWithoutCreatingNewLink() {
        Payment payment = payosPaymentWithLink();
        stubValidContext(order(OrderStatus.AWAITING_PAYMENT), payment);
        stubGetLinkFails();

        assertThatThrownBy(() -> service.createPayosLink(USER_ID, ORDER_ID))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(PaymentErrorCode.PAYOS_CREATE_LINK_FAILED);

        assertThat(payment.getCheckoutUrl()).isEqualTo(OLD_URL);
        verifyNoCreateLink();
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // UTCID11
    @ParameterizedTest
    @EnumSource(value = PaymentLinkStatus.class, names = {"PENDING", "PROCESSING"})
    void createPayosLink_oldLinkStillOpen_shouldReturnOldLink(PaymentLinkStatus status) {
        Payment payment = payosPaymentWithLink();
        stubValidContext(order(OrderStatus.AWAITING_PAYMENT), payment);
        stubGetLink(status);

        PayosLinkResponse result = service.createPayosLink(USER_ID, ORDER_ID);

        assertThat(result).isEqualTo(new PayosLinkResponse(PAYMENT_ID, OLD_URL));
        verifyNoCreateLink();
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(receiptVoucherService, eventPublisher);
    }

    // UTCID12
    @Test
    void createPayosLink_oldLinkAlreadyPaid_shouldMarkPaidAndReturnNullUrl() {
        Payment payment = payosPaymentWithLink();
        stubValidContext(order(OrderStatus.AWAITING_PAYMENT), payment);
        stubGetLink(PaymentLinkStatus.PAID);

        PayosLinkResponse result = service.createPayosLink(USER_ID, ORDER_ID);

        assertThat(result).isEqualTo(new PayosLinkResponse(PAYMENT_ID, null));
        verifyMarkedPaid(payment, ReceiptCategoryType.SALE, null,
                "Thanh toán PayOS (đồng bộ), orderCode=" + EXTERNAL_ID);
        verify(eventPublisher).publishEvent(new PaymentPaidEvent(ORDER_ID));
        verifyNoCreateLink();
    }

    // UTCID13
    @ParameterizedTest
    @EnumSource(value = PaymentLinkStatus.class, names = {"CANCELLED", "EXPIRED"})
    void createPayosLink_oldLinkDead_shouldCreateNewLink(PaymentLinkStatus status) {
        Payment payment = payosPaymentWithLink();
        stubValidContext(order(OrderStatus.AWAITING_PAYMENT), payment);
        stubGetLink(status);
        stubCreateLink(NEW_URL);

        PayosLinkResponse result = service.createPayosLink(USER_ID, ORDER_ID);

        ArgumentCaptor<CreatePaymentLinkRequest> captor = ArgumentCaptor.forClass(CreatePaymentLinkRequest.class);
        verify(payOS.paymentRequests()).create(captor.capture());
        assertThat(payment.getExternalId()).isEqualTo(String.valueOf(captor.getValue().getOrderCode()));
        assertThat(payment.getCheckoutUrl()).isEqualTo(NEW_URL);
        assertThat(result).isEqualTo(new PayosLinkResponse(PAYMENT_ID, NEW_URL));
        verify(paymentRepository).save(payment);
        verifyNoInteractions(receiptVoucherService, eventPublisher);
    }

    // handlePayosWebhook
    // UTCID01
    @Test
    void handlePayosWebhook_invalidSignature_shouldIgnoreAndReturnTrue() throws Exception {
        when(payOS.webhooks().verify(webhook)).thenThrow(new RuntimeException("bad signature"));

        boolean result = service.handlePayosWebhook(webhook);

        assertThat(result).isTrue();
        verifyNoInteractions(paymentRepository, receiptVoucherService, eventPublisher);
    }

    // UTCID02
    @Test
    void handlePayosWebhook_paymentNotFound_shouldReturnFalse() throws Exception {
        stubVerify(webhookData("00", 150000L));
        stubPaymentByExternalId(null);

        boolean result = service.handlePayosWebhook(webhook);

        assertThat(result).isFalse();
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(receiptVoucherService, eventPublisher);
    }

    // UTCID03
    @Test
    void handlePayosWebhook_paymentNotPending_shouldSkipAndReturnTrue() throws Exception {
        Payment payment = payosPaymentWithLink();
        payment.setStatus(PaymentStatus.PAID);
        stubVerify(webhookData("00", 150000L));
        stubPaymentByExternalId(payment);

        boolean result = service.handlePayosWebhook(webhook);

        assertThat(result).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(receiptVoucherService, eventPublisher);
    }

    // UTCID04
    @Test
    void handlePayosWebhook_failureCode_shouldMarkFailed() throws Exception {
        Payment payment = payosPaymentWithLink();
        stubVerify(webhookData("01", 150000L));
        stubPaymentByExternalId(payment);

        boolean result = service.handlePayosWebhook(webhook);

        assertThat(result).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
        verifyNoInteractions(receiptVoucherService, eventPublisher);
    }

    // UTCID05
    @Test
    void handlePayosWebhook_amountMismatch_shouldNotMarkPaid() throws Exception {
        Payment payment = payosPaymentWithLink();
        stubVerify(webhookData("00", 1000L));
        stubPaymentByExternalId(payment);

        boolean result = service.handlePayosWebhook(webhook);

        assertThat(result).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(receiptVoucherService, eventPublisher);
    }

    // UTCID06
    @Test
    void handlePayosWebhook_validPayment_shouldMarkPaidAndPublishEvent() throws Exception {
        Payment payment = payosPaymentWithLink();
        stubVerify(webhookData("00", 150000L));
        stubPaymentByExternalId(payment);

        boolean result = service.handlePayosWebhook(webhook);

        assertThat(result).isTrue();
        verifyMarkedPaid(payment, ReceiptCategoryType.SALE, "FT123", "Thanh toán PayOS, orderCode=" + ORDER_CODE);
        verify(eventPublisher).publishEvent(new PaymentPaidEvent(ORDER_ID));
    }

    // settleCod
    // UTCID01
    @Test
    void settleCod_pendingPayment_shouldMarkPaidWithSettlementReceipt() {
        Payment payment = payment(PaymentMethod.COD, PaymentStatus.PENDING);
        stubPaymentByOrder(payment);

        service.settleCod(order(OrderStatus.SHIPPING));

        verifyMarkedPaid(payment, ReceiptCategoryType.SETTLEMENT, null, "Goship thu hộ đơn COD ORD-001");
    }

    // UTCID02
    @Test
    void settleCod_noPayment_shouldCreateCodPaymentThenMarkPaid() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(PAYMENT_ID);
            return p;
        });

        service.settleCod(order(OrderStatus.SHIPPING));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        Payment saved = captor.getAllValues().get(0);
        assertThat(saved.getType()).isEqualTo(PaymentType.SALE);
        assertThat(saved.getDirection()).isEqualTo(PaymentDirection.IN);
        assertThat(saved.getMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(saved.getAmount()).isEqualByComparingTo("150000");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(receiptVoucherService).createFromPayment(PAYMENT_ID, AMOUNT,
                ReceiptCategoryType.SETTLEMENT, "Goship thu hộ đơn COD ORD-001");
    }

    // UTCID03
    @Test
    void settleCod_alreadyPaid_shouldSkip() {
        Payment payment = payment(PaymentMethod.COD, PaymentStatus.PAID);
        stubPaymentByOrder(payment);

        service.settleCod(order(OrderStatus.SHIPPING));

        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(receiptVoucherService);
    }

    // isPaid
    // UTCID01
    @Test
    void isPaid_paidPayment_shouldReturnTrue() {
        stubPaymentByOrder(payment(PaymentMethod.PAYOS, PaymentStatus.PAID));

        assertThat(service.isPaid(ORDER_ID)).isTrue();
    }

    // UTCID02
    @Test
    void isPaid_pendingPayment_shouldReturnFalse() {
        stubPaymentByOrder(payment(PaymentMethod.PAYOS, PaymentStatus.PENDING));

        assertThat(service.isPaid(ORDER_ID)).isFalse();
    }

    // UTCID03
    @Test
    void isPaid_noPayment_shouldReturnFalse() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        assertThat(service.isPaid(ORDER_ID)).isFalse();
    }

    // onOrderCancelled
    // UTCID01
    @Test
    void onOrderCancelled_noPayment_shouldDoNothing() {
        when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        service.onOrderCancelled(orderForCancel());

        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(payOS);
    }

    // UTCID02
    @Test
    void onOrderCancelled_pendingCod_shouldMarkFailedWithoutCallingPayos() {
        Payment payment = payment(PaymentMethod.COD, PaymentStatus.PENDING);
        stubPaymentByOrder(payment);

        service.onOrderCancelled(orderForCancel());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
        verifyNoInteractions(payOS);
    }

    // UTCID03
    @Test
    void onOrderCancelled_pendingPayosWithoutExternalId_shouldMarkFailedWithoutCallingPayos() {
        Payment payment = payment(PaymentMethod.PAYOS, PaymentStatus.PENDING);
        stubPaymentByOrder(payment);

        service.onOrderCancelled(orderForCancel());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
        verifyNoInteractions(payOS);
    }

    // UTCID04
    @Test
    void onOrderCancelled_pendingPayosCancelSucceeds_shouldMarkFailed() {
        Payment payment = payosPaymentWithLink();
        stubPaymentByOrder(payment);

        service.onOrderCancelled(orderForCancel());

        verify(payOS.paymentRequests()).cancel(ORDER_CODE, CANCEL_REASON);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    // UTCID05
    @ParameterizedTest
    @EnumSource(value = PaymentLinkStatus.class, names = {"CANCELLED", "EXPIRED"})
    void onOrderCancelled_cancelFailsButLinkAlreadyDead_shouldMarkFailed(PaymentLinkStatus status) {
        Payment payment = payosPaymentWithLink();
        stubPaymentByOrder(payment);
        stubCancelFails();
        stubGetLink(status);

        service.onOrderCancelled(orderForCancel());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentRepository).save(payment);
    }

    // UTCID06
    @ParameterizedTest
    @EnumSource(value = PaymentLinkStatus.class, names = {"PENDING", "PAID"})
    void onOrderCancelled_cancelFailsAndLinkStillAlive_shouldKeepPending(PaymentLinkStatus status) {
        Payment payment = payosPaymentWithLink();
        stubPaymentByOrder(payment);
        stubCancelFails();
        stubGetLink(status);

        service.onOrderCancelled(orderForCancel());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // UTCID07
    @Test
    void onOrderCancelled_cancelAndGetBothFail_shouldKeepPending() {
        Payment payment = payosPaymentWithLink();
        stubPaymentByOrder(payment);
        stubCancelFails();
        stubGetLinkFails();

        service.onOrderCancelled(orderForCancel());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    // UTCID08
    @Test
    void onOrderCancelled_paidPayment_shouldLeaveUntouched() {
        Payment payment = payosPaymentWithLink();
        payment.setStatus(PaymentStatus.PAID);
        stubPaymentByOrder(payment);

        service.onOrderCancelled(orderForCancel());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(any(Payment.class));
        verifyNoInteractions(payOS);
    }
}