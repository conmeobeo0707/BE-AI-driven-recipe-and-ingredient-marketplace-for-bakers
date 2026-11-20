package com.manhnv.capstonebakerai.voucher.dto.response;

import com.manhnv.capstonebakerai.voucher.enums.DiscountType;
import com.manhnv.capstonebakerai.voucher.enums.VoucherDisplayStatus;
import com.manhnv.capstonebakerai.voucher.enums.VoucherStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VoucherResponse(
        UUID id,
        String code,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderValue,
        Integer totalQuantity,
        Integer usedQuantity,
        Integer remainingQuantity,
        Integer maxUsagePerCustomer,
        LocalDateTime startDate,
        LocalDateTime endDate,
        VoucherStatus status,
        VoucherDisplayStatus displayStatus,
        UUID createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
