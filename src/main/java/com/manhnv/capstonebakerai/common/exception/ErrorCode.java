package com.manhnv.capstonebakerai.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getMessage();
    HttpStatus getHttpStatus();

    default String getCode() {
        return this instanceof Enum<?> constant ? constant.name() : null;
    }
}
