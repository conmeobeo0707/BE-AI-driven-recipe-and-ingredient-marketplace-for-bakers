package com.manhnv.capstonebakerai.receipt.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateReceiptVoucherRequest(
        @NotNull UUID receiptCategoryId,
        @NotNull @DecimalMin(value = "0.01")BigDecimal amount,
        @Size(max = 500) String note
        ) {
}
