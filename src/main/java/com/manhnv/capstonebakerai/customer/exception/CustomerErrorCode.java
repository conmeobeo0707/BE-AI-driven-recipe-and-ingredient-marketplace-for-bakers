package com.manhnv.capstonebakerai.customer.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CustomerErrorCode implements ErrorCode {
    CUSTOMER_PROFILE_NOT_FOUND("Customer profile was not found", HttpStatus.NOT_FOUND),
    INVALID_HOBBY_SELECTION("One or more selected hobbies are invalid", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;
}
