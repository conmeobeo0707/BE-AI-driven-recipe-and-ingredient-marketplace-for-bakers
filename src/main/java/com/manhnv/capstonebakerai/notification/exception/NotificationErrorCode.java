package com.manhnv.capstonebakerai.notification.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("Notification not found", HttpStatus.NOT_FOUND),
    ;

    private final String message;
    private final HttpStatus httpStatus;
}
