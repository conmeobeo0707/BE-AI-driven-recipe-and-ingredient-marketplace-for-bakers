package com.manhnv.capstonebakerai.voucher.dto.response;

import com.manhnv.capstonebakerai.voucher.enums.DiscountType;

import java.math.BigDecimal;
import java.util.UUID;

public record VoucherPreviewResponse(
        UUID voucherId,
        String code,
        String name,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal orderValue,        // tiền hàng dùng để tính (sau giảm tay của staff)
        BigDecimal discountAmount,    // số tiền voucher giảm
        BigDecimal amountAfterVoucher // orderValue - discountAmount
) {
}
