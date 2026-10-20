package com.manhnv.capstonebakerai.quote.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RequestQuoteErrorCode implements ErrorCode {
    REQUEST_QUOTE_NOT_FOUND("Request quote not found", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND("Customer not found", HttpStatus.NOT_FOUND),
    PRODUCT_UNIT_NOT_FOUND("Product unit not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_AVAILABLE("Product is not available for sale", HttpStatus.BAD_REQUEST),
    DUPLICATE_ITEM("Duplicate product unit in request", HttpStatus.BAD_REQUEST),
    REQUEST_QUOTE_INVALID_STATUS_TRANSITION("Invalid request quote status transition", HttpStatus.BAD_REQUEST),
    REQUEST_QUOTE_CANNOT_EDIT("Request quote can only be edited while pending", HttpStatus.BAD_REQUEST),
    REQUEST_QUOTE_CANNOT_CANCEL("Request quote cannot be cancelled in its current status", HttpStatus.BAD_REQUEST),
    REQUEST_QUOTE_ALREADY_HANDLED("Request quote is already being handled", HttpStatus.CONFLICT),
    REQUEST_QUOTE_NOT_HANDLER("Only the handler or a manager can perform this action", HttpStatus.FORBIDDEN);

    private final String message;
    private final HttpStatus httpStatus;
}
