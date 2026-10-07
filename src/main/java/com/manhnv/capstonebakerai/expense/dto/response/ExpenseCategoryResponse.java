package com.manhnv.capstonebakerai.expense.dto.response;

import com.manhnv.capstonebakerai.expense.enums.ExpenseCategoryType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseCategoryResponse(
        UUID id,
        String name,
        ExpenseCategoryType type,
        LocalDateTime createdAt)
{ }
