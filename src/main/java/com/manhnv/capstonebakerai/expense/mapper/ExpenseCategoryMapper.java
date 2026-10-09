package com.manhnv.capstonebakerai.expense.mapper;

import com.manhnv.capstonebakerai.expense.dto.response.ExpenseCategoryResponse;
import com.manhnv.capstonebakerai.expense.entity.ExpenseCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseCategoryMapper {
    ExpenseCategoryResponse toResponse(ExpenseCategory entity);
}
