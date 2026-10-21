package com.manhnv.capstonebakerai.quote.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RequestQuoteItemRequest(
        @NotNull UUID productUnitId,
        @Min(1) int quantity
        ) {
}
