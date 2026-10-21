package com.manhnv.capstonebakerai.quote.dto.response;

import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RequestQuoteSummaryResponse(
        UUID id,
        UUID customerId,
        String contactPhone,
        String contactName,
        RequestQuoteStatus status,
        int itemCount,
        UUID handledBy,
        LocalDateTime createdAt
) {
}
