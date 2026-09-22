package com.manhnv.capstonebakerai.common.exception;

import com.manhnv.capstonebakerai.identity.exception.EmailAlreadyExistsException;
import com.manhnv.capstonebakerai.identity.exception.InvalidRegistrationRoleException;
import com.manhnv.capstonebakerai.identity.exception.InvalidVerificationCodeException;
import com.manhnv.capstonebakerai.identity.exception.VerificationEmailDeliveryException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                errors,
                request
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_EXISTS",
                exception.getMessage(),
                Collections.emptyMap(),
                request
        );
    }

    @ExceptionHandler(InvalidRegistrationRoleException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidRegistrationRole(
            InvalidRegistrationRoleException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REGISTRATION_ROLE",
                exception.getMessage(),
                Collections.emptyMap(),
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpServletRequest request) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Request body is invalid or contains an unsupported value",
                Collections.emptyMap(),
                request
        );
    }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    ResponseEntity<ApiErrorResponse> handleInvalidVerificationCode(
            InvalidVerificationCodeException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_VERIFICATION_CODE",
                exception.getMessage(),
                Collections.emptyMap(),
                request
        );
    }

    @ExceptionHandler(VerificationEmailDeliveryException.class)
    ResponseEntity<ApiErrorResponse> handleVerificationEmailDelivery(
            VerificationEmailDeliveryException exception,
            HttpServletRequest request
    ) {
        return errorResponse(
                HttpStatus.BAD_GATEWAY,
                "VERIFICATION_EMAIL_DELIVERY_FAILED",
                exception.getMessage(),
                Collections.emptyMap(),
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> errorResponse(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> errors,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                status.value(),
                code,
                message,
                errors,
                request.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(response);
    }
}
