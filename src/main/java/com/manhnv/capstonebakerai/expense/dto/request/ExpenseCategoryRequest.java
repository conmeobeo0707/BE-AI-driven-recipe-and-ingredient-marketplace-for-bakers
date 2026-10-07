package com.manhnv.capstonebakerai.expense.dto.request;

import com.manhnv.capstonebakerai.expense.enums.ExpenseCategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExpenseCategoryRequest (
    @NotBlank @Size(max = 150) String name,
    @NotNull ExpenseCategoryType type) {}
