package com.manhnv.capstonebakerai.warehouse.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public record StockInCreateRequest(@NotNull UUID supplierId, LocalDateTime receiptDate,
        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal paidAmount,
        @Size(max = 5000) String note,
        @NotEmpty List<@Valid StockInItemRequest> items) { }
