package com.manhnv.capstonebakerai.quote.dto.response;

import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record RequestQuoteSummaryResponse(
        UUID id,
        UUID customerId,
        String contactPhone,
        String contactName,
        RequestQuoteStatus status,
        int itemCount,
        UserSummaryResponse handledBy,
        LocalDateTime createdAt
) {
}
