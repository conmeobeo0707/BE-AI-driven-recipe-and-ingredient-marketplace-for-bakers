package com.manhnv.capstonebakerai.quote.enums;


import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum RequestQuoteStatus {
    PENDING,
    IN_PROGRESS,
    ORDERED,
    REJECTED,
    CANCELLED;

    private static final Map<RequestQuoteStatus, Set<RequestQuoteStatus>> TRANSITIONS = new EnumMap<>(RequestQuoteStatus.class);

    static {
        TRANSITIONS.put(PENDING, Set.of(IN_PROGRESS, REJECTED, CANCELLED));
        TRANSITIONS.put(IN_PROGRESS, Set.of(ORDERED, REJECTED, CANCELLED));
        TRANSITIONS.put(ORDERED, Set.of());
        TRANSITIONS.put(REJECTED, Set.of());
        TRANSITIONS.put(CANCELLED, Set.of());
    }

    private static final Set<RequestQuoteStatus> CUSTOMER_CANCELLABLE = Set.of(PENDING, IN_PROGRESS);

    public boolean canTransitionTo(RequestQuoteStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public boolean isCancellableByCustomer() {
        return CUSTOMER_CANCELLABLE.contains(this);
    }

    public boolean isEditableByCustomer() {
        return this == PENDING;
    }
}
