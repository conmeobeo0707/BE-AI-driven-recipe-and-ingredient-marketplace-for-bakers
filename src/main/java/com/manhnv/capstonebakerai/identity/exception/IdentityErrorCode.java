package com.manhnv.capstonebakerai.identity.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IdentityErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS("Email is already registered", HttpStatus.CONFLICT),
    INVALID_REGISTRATION_ROLE("Self-registration only supports LEARNER, STORE, or BAKER roles", HttpStatus.BAD_REQUEST),
    INVALID_VERIFICATION_CODE("Verification code is invalid or expired", HttpStatus.BAD_REQUEST),
    VERIFICATION_EMAIL_DELIVERY_FAILED("Verification email could not be sent", HttpStatus.BAD_GATEWAY);

    private final String message;
    private final HttpStatus httpStatus;
}
