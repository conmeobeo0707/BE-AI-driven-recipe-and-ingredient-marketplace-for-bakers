package com.manhnv.capstonebakerai.expense.dto.response;

import com.manhnv.capstonebakerai.expense.enums.ExpenseVoucherStatus;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseVoucherResponse(
        UUID id,
        String code,
        UUID expenseCategoryId,
        String expenseCategoryName,
        BigDecimal amount,
        String reason,
        ExpenseVoucherStatus status,
        UserSummaryResponse createdBy,
        LocalDateTime createdAt)
{ }
