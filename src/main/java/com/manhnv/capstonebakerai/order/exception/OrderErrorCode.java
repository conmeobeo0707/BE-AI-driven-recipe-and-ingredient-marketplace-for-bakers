package com.manhnv.capstonebakerai.order.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("Order not found", HttpStatus.NOT_FOUND),
    CUSTOMER_NOT_FOUND("Customer not found", HttpStatus.NOT_FOUND),
    PRODUCT_UNIT_NOT_FOUND("Product unit not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_AVAILABLE("Product is not available for sale", HttpStatus.BAD_REQUEST),
    ORDER_INVALID_STATUS_TRANSITION("Invalid order status transition", HttpStatus.BAD_REQUEST),
    ORDER_USE_CANCEL_ENDPOINT("Use the cancel endpoint to cancel an order", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_CANCEL("Order cannot be cancelled in its current status", HttpStatus.BAD_REQUEST),
    ORDER_NOT_DELIVERED("Order has not been delivered yet", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE("Invalid date range", HttpStatus.BAD_REQUEST),
    PRODUCT_WEIGHT_MISSING("Product unit has no weight configured", HttpStatus.INTERNAL_SERVER_ERROR)
    ;

    private final String message;
    private final HttpStatus httpStatus;
}
