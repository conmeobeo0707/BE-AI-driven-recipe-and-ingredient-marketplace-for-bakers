package com.manhnv.capstonebakerai.quote.event;

import java.util.UUID;

public record RequestQuoteCreatedEvent(
        UUID requestQuoteId,
        String contactName,
        String contactPhone
) {
}
