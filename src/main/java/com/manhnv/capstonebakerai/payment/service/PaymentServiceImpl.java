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
import com.manhnv.capstonebakerai.payment.entity.PaymentTransaction;
import com.manhnv.capstonebakerai.payment.enums.PaymentStatus;
import com.manhnv.capstonebakerai.payment.enums.TransactionDirection;
import com.manhnv.capstonebakerai.payment.enums.TransactionMethod;
import com.manhnv.capstonebakerai.payment.enums.TransactionRefType;
import com.manhnv.capstonebakerai.payment.event.PaymentPaidEvent;
import com.manhnv.capstonebakerai.payment.exception.PaymentErrorCode;
import com.manhnv.capstonebakerai.payment.repository.PaymentRepository;
import com.manhnv.capstonebakerai.payment.repository.PaymentTransactionRepository;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import com.manhnv.capstonebakerai.receipt.service.ReceiptVoucherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    PaymentTransactionRepository transactionRepository;
    OrderRepository orderRepository;
    CustomerRepository customerRepository;
    ReceiptVoucherService receiptVoucherService;
    ApplicationEventPublisher eventPublisher;
    PayOS payOS;

    @NonFinal @Value("${payos.return-url}") String returnUrl;
    @NonFinal @Value("${payos.cancel-url}") String cancelUrl;

    private static final String PAYOS_SUCCESS_CODE = "00";

    @Override
    @Transactional
    public void createPending(UUID orderId, PaymentMethod method, BigDecimal amount) {
        paymentRepository.save(Payment.builder()
                        .orderId(orderId)
                        .method(method)
                        .amount(amount)
                        .status(PaymentStatus.PENDING)
                .build());
    }

    @Override
    @Transactional
    public PayosLinkResponse createPayosLink(UUID userId, UUID orderId) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.CUSTOMER_NOT_FOUND));
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getCustomer().getId().equals(customer.getId()))
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getMethod() != PaymentMethod.PAYOS) throw new AppException(PaymentErrorCode.PAYMENT_NOT_PAYOS);
        if (payment.getStatus() != PaymentStatus.PENDING) throw new AppException(PaymentErrorCode.PAYMENT_NOT_PENDING);
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION);

        if (payment.getCheckoutUrl() != null) {
            PaymentLink link;
            try {
                link = payOS.paymentRequests().get(Long.parseLong(payment.getExternalId()));
            } catch (PayOSException e) {
                // không biết link cũ ra sao thì không tạo link mới, tránh khách trả tiền hai lần
                log.warn("PayOS get link failed, payment={}", payment.getId(), e);
                throw new AppException(PaymentErrorCode.PAYOS_CREATE_LINK_FAILED);
            }

            switch (link.getStatus()) {
                case PENDING, PROCESSING -> {
                    return new PayosLinkResponse(payment.getId(), payment.getCheckoutUrl());
                }
                case PAID -> {
                    markPaid(payment, TransactionMethod.BANK_TRANSFER, ReceiptCategoryType.SALE,
                            null, "Thanh toán PayOS (đồng bộ), orderCode=" + payment.getExternalId());
                    eventPublisher.publishEvent(new PaymentPaidEvent(payment.getOrderId()));
                    return new PayosLinkResponse(payment.getId(), null);   // null = đã thanh toán rồi
                }
                default -> {   // CANCELLED, EXPIRED
                    payment.setExternalId(null);
                    payment.setCheckoutUrl(null);
                    // rơi xuống dưới để tạo link mới
                }
            }
        }

        long orderCode = System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000);
        try {
            CreatePaymentLinkResponse res = payOS.paymentRequests().create(
                    CreatePaymentLinkRequest.builder()
                            .orderCode(orderCode)
                            .amount(payment.getAmount().longValue())
                            .description("Thanh toán " + order.getCode())   // PayOS giới hạn ~25 ký tự
                            .returnUrl(returnUrl)
                            .cancelUrl(cancelUrl)
                            .build());
            payment.setExternalId(String.valueOf(orderCode));
            payment.setCheckoutUrl(res.getCheckoutUrl());
            paymentRepository.save(payment);
            return new PayosLinkResponse(payment.getId(), res.getCheckoutUrl());
        } catch (PayOSException e) {
            log.error("PayOS create link failed, order={}", order.getCode(), e);
            throw new AppException(PaymentErrorCode.PAYOS_CREATE_LINK_FAILED);
        }
    }

    @Override
    @Transactional
    public boolean handlePayosWebhook(Webhook webhook) {
        WebhookData data;
        try {
            data = payOS.webhooks().verify(webhook);
        } catch (Exception e) {
            log.warn("Invalid PayOS webhook signature: {}", e.getMessage());
            return true;   // đã xử lý (bỏ qua)
        }

        Payment payment = paymentRepository
                .findByExternalIdForUpdate(String.valueOf(data.getOrderCode())).orElse(null);
        if (payment == null) return false;

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Payment [{}] already {}, skip", payment.getId(), payment.getStatus());
            return true;
        }
        if (!PAYOS_SUCCESS_CODE.equals(data.getCode())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return true;
        }
        if (payment.getAmount().longValue() != data.getAmount()) {
            log.error("Payment [{}] amount mismatch: expected {}, got {}",
                    payment.getId(), payment.getAmount(), data.getAmount());
            return true;                                   // không đánh dấu PAID, cần đối soát tay
        }

        markPaid(payment, TransactionMethod.BANK_TRANSFER, ReceiptCategoryType.SALE,
                data.getReference(), "Thanh toán PayOS, orderCode=" + data.getOrderCode());
        eventPublisher.publishEvent(new PaymentPaidEvent(payment.getOrderId()));
        return true;
    }

    @Override
    @Transactional
    public void settleCod(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseGet(() -> paymentRepository.save(Payment.builder()
                        .orderId(order.getId()).method(PaymentMethod.COD)
                        .amount(order.getTotal()).status(PaymentStatus.PENDING).build()));
        if (payment.getStatus() == PaymentStatus.PAID) return;   // webhook Goship bắn lặp
        markPaid(payment, TransactionMethod.BANK_TRANSFER, ReceiptCategoryType.SETTLEMENT,
                null, "Goship thu hộ đơn COD " + order.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPaid(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(p -> p.getStatus() == PaymentStatus.PAID).orElse(false);
    }

    @Override
    @Transactional
    public void onOrderCancelled(Order order) {
        paymentRepository.findByOrderId(order.getId()).ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(p);
            } else if (p.getStatus() == PaymentStatus.PAID) {
                // TODO: hoàn tiền thủ công cho khách (đơn PayOS đã thanh toán)
                log.warn("Order {} cancelled but payment PAID, needs refund", order.getCode());
            }
        });
    }

    private void markPaid(Payment payment, TransactionMethod method, ReceiptCategoryType receiptType,
                          String externalRef, String note) {
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        transactionRepository.save(PaymentTransaction.builder()
                .refType(TransactionRefType.PAYMENT).refId(payment.getId())
                .direction(TransactionDirection.IN).method(method)
                .amount(payment.getAmount()).externalId(externalRef).note(note).build());

        receiptVoucherService.createFromPayment(payment.getId(), payment.getAmount(), receiptType, note);
    }
}
