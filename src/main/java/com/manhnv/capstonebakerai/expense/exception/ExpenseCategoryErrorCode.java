package com.manhnv.capstonebakerai.expense.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExpenseCategoryErrorCode implements ErrorCode {
    EXPENSE_CATEGORY_NOT_FOUND("Expense category not found", HttpStatus.NOT_FOUND),
    EXPENSE_CATEGORY_NAME_EXISTED("Expense category already existed", HttpStatus.CONFLICT),
    EXPENSE_CATEGORY_IN_USE("Expense category in use", HttpStatus.BAD_REQUEST),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}
