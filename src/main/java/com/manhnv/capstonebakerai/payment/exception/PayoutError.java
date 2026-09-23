package com.manhnv.capstonebakerai.payment.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PayoutError implements ErrorCode {
    PAYOUT_NOT_FOUND("Payout not found", HttpStatus.NOT_FOUND),
    PAYOUT_ALREADY_PROCESSED("Payout already processed", HttpStatus.BAD_REQUEST),
    PAYOUT_PROCESSING_FAILED("Failed to process payout payment", HttpStatus.BAD_REQUEST),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}