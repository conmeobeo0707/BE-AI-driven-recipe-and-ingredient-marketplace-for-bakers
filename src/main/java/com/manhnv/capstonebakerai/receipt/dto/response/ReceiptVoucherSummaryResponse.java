package com.manhnv.capstonebakerai.receipt.dto.response;

import java.math.BigDecimal;

public record ReceiptVoucherSummaryResponse(
        long totalVouchers,
        BigDecimal totalAmount,
        BigDecimal averagePerVoucher,
        long myVoucherCount
) {}
