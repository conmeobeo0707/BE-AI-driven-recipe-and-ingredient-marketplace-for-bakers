package com.manhnv.capstonebakerai.user.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("User was not found", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("Email is already registered", HttpStatus.CONFLICT),
    ACCOUNT_NOT_FOUND("Managed staff account was not found", HttpStatus.NOT_FOUND),
    INVALID_STAFF_ROLE("Only STAFF, MANAGER and BAKER_STAFF roles are supported", HttpStatus.BAD_REQUEST),
    AVATAR_REQUIRED("Avatar file is required", HttpStatus.BAD_REQUEST),
    INVALID_AVATAR_TYPE("Avatar must be JPEG, PNG or WebP", HttpStatus.BAD_REQUEST),
    AVATAR_TOO_LARGE("Avatar must not exceed 5 MB", HttpStatus.BAD_REQUEST),
    AVATAR_STORAGE_FAILED("Avatar storage failed", HttpStatus.BAD_GATEWAY);

    private final String message;
    private final HttpStatus httpStatus;
}
