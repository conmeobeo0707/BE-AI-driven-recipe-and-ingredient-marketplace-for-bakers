package com.manhnv.capstonebakerai.order.enums;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    AWAITING_PAYMENT,
    PENDING,
    CONFIRMED,
    READY_TO_SHIP,
    SHIPPING,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(AWAITING_PAYMENT, Set.of(PENDING, CANCELLED));
        TRANSITIONS.put(PENDING, Set.of(CONFIRMED, CANCELLED));
        TRANSITIONS.put(CONFIRMED, Set.of(READY_TO_SHIP, CANCELLED));
        TRANSITIONS.put(READY_TO_SHIP, Set.of(SHIPPING, CANCELLED));
        TRANSITIONS.put(SHIPPING, Set.of(DELIVERED));
        TRANSITIONS.put(DELIVERED, Set.of());
        TRANSITIONS.put(CANCELLED, Set.of());
    }

    private static final Set<OrderStatus> CUSTOMER_CANCELLABLE = Set.of(AWAITING_PAYMENT, PENDING, CONFIRMED);
    private static final Set<OrderStatus> STAFF_CANCELLABLE = Set.of(AWAITING_PAYMENT, PENDING, CONFIRMED, READY_TO_SHIP);

    public boolean canTransitionTo(OrderStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public boolean isCancellableByCustomer() {
        return CUSTOMER_CANCELLABLE.contains(this);
    }

    public boolean isCancellableByStaff() {
        return STAFF_CANCELLABLE.contains(this);
    }
}
