package com.manhnv.capstonebakerai.warehouse.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
public record StockOutItemRequest(@NotNull UUID productId, @NotNull UUID unitId,
        UUID lotId, @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity) { }
