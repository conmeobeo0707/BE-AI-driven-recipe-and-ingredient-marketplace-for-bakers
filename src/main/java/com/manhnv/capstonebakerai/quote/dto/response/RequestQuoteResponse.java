package com.manhnv.capstonebakerai.quote.dto.response;

import com.manhnv.capstonebakerai.quote.enums.RequestQuoteStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RequestQuoteResponse(
        UUID id,
        UUID customerId,
        String contactPhone,
        String contactName,
        String content,
        RequestQuoteStatus status,
        UUID handledBy,
        LocalDateTime handledAt,
        String rejectReason,
        String resultNote,
        LocalDateTime createdAt,
        List<RequestQuoteItemResponse> items
) {
}
