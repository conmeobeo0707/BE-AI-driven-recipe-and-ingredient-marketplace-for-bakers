package com.manhnv.capstonebakerai.order.dto.response;

import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderTrackingResponse(
        UUID id,
        String code,
        OrderStatus status,
        PaymentMethod paymentMethod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt
) {
}
