package com.manhnv.capstonebakerai.order.dto.response;

import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String code,
        UUID customerId,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal total,
        String receiverName,
        LocalDateTime createdAt
) {
}
