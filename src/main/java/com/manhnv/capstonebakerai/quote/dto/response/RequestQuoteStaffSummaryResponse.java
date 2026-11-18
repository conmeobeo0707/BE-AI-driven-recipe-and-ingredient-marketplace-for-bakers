package com.manhnv.capstonebakerai.quote.dto.response;

public record RequestQuoteStaffSummaryResponse(
        long totalRequests,
        long waitingClaim,
        long handledByMe,
        long ordered
) {}
