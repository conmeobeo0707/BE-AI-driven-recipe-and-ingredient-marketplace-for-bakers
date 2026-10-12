package com.manhnv.capstonebakerai.warehouse.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record StocktakeCountRequest(@NotEmpty List<@Valid CountedLot> items) {
    public record CountedLot(@NotNull UUID lotId,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal actualQuantity,
            @Size(max = 255) String reason, @Size(max = 5000) String note) { }
}
