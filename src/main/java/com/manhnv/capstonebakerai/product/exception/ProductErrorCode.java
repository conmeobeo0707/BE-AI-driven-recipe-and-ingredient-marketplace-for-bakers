package com.manhnv.capstonebakerai.product.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter @AllArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    NOT_FOUND("Product or category was not found", HttpStatus.NOT_FOUND),
    DUPLICATE_SKU("SKU is already in use", HttpStatus.CONFLICT),
    INVALID_REQUEST("Product request is invalid", HttpStatus.BAD_REQUEST),
    CONFLICT("Product cannot be changed because it conflicts with existing data", HttpStatus.CONFLICT);
    private final String message;
    private final HttpStatus httpStatus;
}
