package com.manhnv.capstonebakerai.order.event;

import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, String orderCode) {
}
