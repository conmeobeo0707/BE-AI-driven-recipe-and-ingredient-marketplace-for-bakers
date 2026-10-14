package com.manhnv.capstonebakerai.receipt.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReceiptCategoryErrorCode implements ErrorCode {
    RECEIPT_CATEGORY_NOT_FOUND("Receipt category not found", HttpStatus.NOT_FOUND),
    RECEIPT_CATEGORY_NAME_EXISTED("Receipt category name already exists", HttpStatus.CONFLICT),
    RECEIPT_CATEGORY_IN_USE("Receipt category is in use by vouchers", HttpStatus.CONFLICT),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}