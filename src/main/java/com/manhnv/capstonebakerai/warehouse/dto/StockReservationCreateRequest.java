package com.manhnv.capstonebakerai.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StockReservationCreateRequest(
        @NotNull UUID orderId,
        @NotNull @Future LocalDateTime expiresAt,
        @NotEmpty List<@Valid Item> items) {
    public record Item(@NotNull UUID productId, @NotNull UUID unitId,
                       @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity) { }
}
