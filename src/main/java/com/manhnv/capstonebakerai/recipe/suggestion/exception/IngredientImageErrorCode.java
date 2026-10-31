package com.manhnv.capstonebakerai.recipe.suggestion.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IngredientImageErrorCode implements ErrorCode {

    IMAGE_REQUIRED(
            "An ingredient image is required",
            HttpStatus.BAD_REQUEST
    ),

    IMAGE_TOO_LARGE(
            "Image must not exceed 5 MB",
            HttpStatus.PAYLOAD_TOO_LARGE
    ),

    UNSUPPORTED_IMAGE_TYPE(
            "Only JPEG and PNG images are supported",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE
    ),

    INVALID_IMAGE(
            "Image is invalid or cannot be read",
            HttpStatus.BAD_REQUEST
    ),

    IMAGE_DIMENSIONS_TOO_LARGE(
            "Image dimensions are too large",
            HttpStatus.BAD_REQUEST
    ),

    VISION_NOT_CONFIGURED(
            "Ingredient image recognition is not available",
            HttpStatus.SERVICE_UNAVAILABLE
    ),

    VISION_UNAVAILABLE(
            "Image recognition is temporarily unavailable. Please try again later",
            HttpStatus.SERVICE_UNAVAILABLE
    ),

    VISION_TIMEOUT(
            "Image recognition timed out. Please try again",
            HttpStatus.GATEWAY_TIMEOUT
    ),

    VISION_INVALID_RESPONSE(
            "Image recognition returned an invalid result. Please try another image",
            HttpStatus.BAD_GATEWAY
    ),

    VISION_REFUSED(
            "This image could not be processed. Please use another ingredient image",
            HttpStatus.UNPROCESSABLE_ENTITY
    ),

    IMAGE_RATE_LIMIT_EXCEEDED(
            "Too many image recognition requests. Please try again later",
            HttpStatus.TOO_MANY_REQUESTS
    );

    private final String message;

    private final HttpStatus httpStatus;
}