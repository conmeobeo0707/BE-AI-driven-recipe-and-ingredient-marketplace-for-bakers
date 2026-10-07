package com.manhnv.capstonebakerai.expense.mapper;

import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherResponse;
import com.manhnv.capstonebakerai.expense.entity.ExpenseVoucher;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface ExpenseVoucherMapper {
    @Mapping(source = "expenseCategory.id", target = "expenseCategoryId")
    @Mapping(source = "expenseCategory.name", target = "expenseCategoryName")
    ExpenseVoucherResponse toResponse(ExpenseVoucher entity);
}
