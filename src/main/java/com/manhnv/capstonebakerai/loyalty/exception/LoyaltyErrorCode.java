package com.manhnv.capstonebakerai.loyalty.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LoyaltyErrorCode implements ErrorCode {

    INSUFFICIENT_POINTS(
            "Not enough points for this feature",
            HttpStatus.PAYMENT_REQUIRED
    ),

    AI_USAGE_NOT_FOUND(
            "AI usage not found",
            HttpStatus.NOT_FOUND
    );

    private final String message;

    private final HttpStatus httpStatus;
}
