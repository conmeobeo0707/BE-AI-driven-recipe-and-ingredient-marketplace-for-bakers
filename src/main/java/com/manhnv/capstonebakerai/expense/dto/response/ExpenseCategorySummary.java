package com.manhnv.capstonebakerai.expense.dto.response;

import com.manhnv.capstonebakerai.expense.enums.ExpenseCategoryType;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseCategorySummary(
        UUID categoryId,
        String categoryName,
        ExpenseCategoryType categoryType,
        BigDecimal totalAmount,
        Long voucherCount
) {}
