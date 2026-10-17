package com.manhnv.capstonebakerai.receipt.dto.response;

import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReceiptCategoryResponse(
        UUID id,
        String name,
        ReceiptCategoryType type,
        LocalDateTime createdAt
) {
}
