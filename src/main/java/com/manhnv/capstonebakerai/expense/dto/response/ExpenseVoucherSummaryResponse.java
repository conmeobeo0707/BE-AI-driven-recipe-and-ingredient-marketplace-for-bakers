package com.manhnv.capstonebakerai.expense.dto.response;

import java.math.BigDecimal;

public record ExpenseVoucherSummaryResponse(
        BigDecimal totalAmount,
        long voucherCount,
        long cancelledCount,
        BigDecimal averagePerVoucher
) {}
