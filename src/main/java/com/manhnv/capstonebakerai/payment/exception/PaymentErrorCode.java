package com.manhnv.capstonebakerai.payment.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_FOUND("Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_PAYOS("Payment method invalid", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_PENDING("Payment status invalid", HttpStatus.BAD_REQUEST),
    PAYOS_CREATE_LINK_FAILED("Payos create link failed", HttpStatus.BAD_REQUEST),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}
