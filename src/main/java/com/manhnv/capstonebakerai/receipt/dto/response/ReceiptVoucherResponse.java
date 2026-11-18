package com.manhnv.capstonebakerai.receipt.dto.response;

import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReceiptVoucherResponse(
        UUID id,
        String code,
        UUID receiptCategoryId,
        String receiptCategoryName,
        ReceiptCategoryType receiptCategoryType,
        UUID paymentId,
        BigDecimal amount,
        String note,
        UserSummaryResponse createdBy,
        LocalDateTime createdAt) {
}
