package com.manhnv.capstonebakerai.common.exception;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        int status,
        String code,
        String message,
        Map<String, String> errors,
        String path,
        Instant timestamp
) {
}
