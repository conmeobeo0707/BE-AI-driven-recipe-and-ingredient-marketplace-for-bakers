package com.manhnv.capstonebakerai.expense.dto.response;

import com.manhnv.capstonebakerai.expense.enums.ExpenseVoucherStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Raw projection used only inside the repository's JPQL constructor-expression (before the
 * creator id is enriched into a {@link com.manhnv.capstonebakerai.user.dto.UserSummaryResponse}
 * at the service layer) — not exposed outside {@code ExpenseVoucherServiceImpl}. */
public record ExpenseVoucherRow(
        UUID id,
        String code,
        UUID expenseCategoryId,
        String expenseCategoryName,
        BigDecimal amount,
        String reason,
        ExpenseVoucherStatus status,
        UUID createdBy,
        LocalDateTime createdAt)
{ }
