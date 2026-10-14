package com.manhnv.capstonebakerai.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID productUnitId,
        @NotNull @Min(1) @Max(1000) Integer quantity
        ) {
}
