package com.manhnv.capstonebakerai.expense.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExpenseVoucherErrorCode implements ErrorCode {
    EXPENSE_VOUCHER_NOT_FOUND("Expense voucher not found", HttpStatus.NOT_FOUND),
    EXPENSE_VOUCHER_ALREADY_CANCELLED("Expense voucher already cancelled", HttpStatus.CONFLICT),
    EXPENSE_VOUCHER_CANNOT_UPDATE("Expense voucher cannot update", HttpStatus.BAD_REQUEST),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}
