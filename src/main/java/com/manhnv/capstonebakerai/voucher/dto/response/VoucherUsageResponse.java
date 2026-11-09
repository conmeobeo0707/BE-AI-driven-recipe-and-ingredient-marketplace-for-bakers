package com.manhnv.capstonebakerai.voucher.dto.response;

import com.manhnv.capstonebakerai.voucher.enums.VoucherUsageStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VoucherUsageResponse(
        UUID id,
        UUID voucherId,
        UUID customerId,
        UUID orderId,
        BigDecimal discountAmount,
        VoucherUsageStatus status,
        LocalDateTime usedAt,
        LocalDateTime refundedAt
) {
}
