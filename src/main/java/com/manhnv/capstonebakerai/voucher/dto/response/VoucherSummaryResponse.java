package com.manhnv.capstonebakerai.voucher.dto.response;

import com.manhnv.capstonebakerai.voucher.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VoucherSummaryResponse(
        UUID id,
        String code,
        String name,
        String description,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderValue,
        Integer remainingQuantity,
        Integer maxUsagePerCustomer,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
