package com.manhnv.capstonebakerai.order.service;

import com.manhnv.capstonebakerai.order.enums.OrderActorType;

import java.util.UUID;

public record OrderActor(OrderActorType type, UUID userId) {
    public static OrderActor system() {
        return new OrderActor(OrderActorType.SYSTEM, null);
    }
}
