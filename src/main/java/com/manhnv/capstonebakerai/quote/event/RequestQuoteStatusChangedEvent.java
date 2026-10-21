package com.manhnv.capstonebakerai.quote.event;

import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;

import java.util.UUID;

public record RequestQuoteStatusChangedEvent(
        UUID requestQuoteId,
        UUID customerUserId,
        RequestQuoteStatus newStatus,
        UUID handledBy,
        String reason,
        boolean cancelledByCustomer
) {
}
