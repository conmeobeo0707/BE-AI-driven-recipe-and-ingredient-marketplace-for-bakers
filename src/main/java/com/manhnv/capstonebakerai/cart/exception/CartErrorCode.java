package com.manhnv.capstonebakerai.cart.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CartErrorCode implements ErrorCode {
    CART_ITEM_NOT_FOUND("Cart item not found", HttpStatus.NOT_FOUND),
    CART_PRODUCT_NOT_FOUND("Product not found", HttpStatus.NOT_FOUND),
    CART_UNIT_NOT_FOUND("Product unit not found", HttpStatus.NOT_FOUND),
    CART_UNIT_NOT_AVAILABLE("Unit is not available for sale", HttpStatus.BAD_REQUEST),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}