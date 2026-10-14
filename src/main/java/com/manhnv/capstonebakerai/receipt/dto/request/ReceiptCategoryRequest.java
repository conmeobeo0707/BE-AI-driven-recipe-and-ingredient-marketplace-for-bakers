package com.manhnv.capstonebakerai.receipt.dto.request;

import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReceiptCategoryRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull ReceiptCategoryType type
        ) {
}
