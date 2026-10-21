package com.manhnv.capstonebakerai.order.event;

import com.manhnv.capstonebakerai.order.enums.OrderStatus;

import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        String orderCode,
        UUID customerUserId,
        OrderStatus newStatus,
        boolean cancelledByCustomer
) {
}
