package com.manhnv.capstonebakerai.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record StaffCreateOrderRequest(
        @NotNull UUID customerId,
        @DecimalMin("0") @DecimalMax("100")BigDecimal discountPercent,
        @NotNull @Valid CreateOrderRequest order
        ) {
}
