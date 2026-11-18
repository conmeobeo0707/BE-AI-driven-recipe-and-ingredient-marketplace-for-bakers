package com.manhnv.capstonebakerai.expense.dto.response;

public record ExpenseVoucherWithNameResponse(
        ExpenseVoucherResponse response,
        String staffName
) {
}
