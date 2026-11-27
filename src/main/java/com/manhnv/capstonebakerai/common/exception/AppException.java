package com.manhnv.capstonebakerai.common.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final transient Object data;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public AppException(ErrorCode errorCode, String customMessage) {
        this(errorCode, customMessage, null);
    }

    public AppException(ErrorCode errorCode, String customMessage, Object data) {
        super(customMessage);
        this.errorCode = errorCode;
        this.data = data;
    }
}
