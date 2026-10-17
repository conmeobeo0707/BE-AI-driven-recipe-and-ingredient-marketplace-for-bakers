package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.payment.dto.response.PayosLinkResponse;
import vn.payos.model.webhooks.Webhook;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    void createPending(UUID orderId, PaymentMethod method, BigDecimal amount);
    PayosLinkResponse createPayosLink(UUID userId, UUID orderId);
    boolean handlePayosWebhook(Webhook webhook);
    void settleCod(Order order);
    boolean isPaid(UUID orderId);
    void onOrderCancelled(Order order);

}

