package com.manhnv.capstonebakerai.customer.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomerAddressErrorCode implements ErrorCode {
    CUSTOMER_NOT_FOUND("Customer not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND("Address not found", HttpStatus.NOT_FOUND),
    ADDRESS_LIMIT_REACHED("Maximum number of address reached", HttpStatus.BAD_REQUEST)
    ;

    private final String message;
    private final HttpStatus httpStatus;
}
