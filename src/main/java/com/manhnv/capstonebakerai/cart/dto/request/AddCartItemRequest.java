package com.manhnv.capstonebakerai.cart.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddCartItemRequest(
        @NotNull UUID productId,
        @NotNull UUID uomId,
        @NotNull @DecimalMin(value = "0", inclusive = false, message = "Quantity must greater than 0")
        @Digits(integer = 10, fraction = 3)
        BigDecimal quantity
        ) {
}
