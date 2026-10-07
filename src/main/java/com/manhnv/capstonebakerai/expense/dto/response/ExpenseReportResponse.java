package com.manhnv.capstonebakerai.expense.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpenseReportResponse(
        String month,
        UUID categoryId,
        BigDecimal totalAmount,
        long totalVouchers,
        List<ExpenseCategorySummary> categories
) {}
