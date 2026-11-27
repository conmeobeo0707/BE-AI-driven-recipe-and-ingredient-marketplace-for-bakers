package com.manhnv.capstonebakerai.videoextraction.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecipeVideoExtractionErrorCode implements ErrorCode {

    EXTRACTION_NOT_CONFIGURED(
            "Recipe extraction from video is not available",
            HttpStatus.SERVICE_UNAVAILABLE
    ),

    MEDIA_NOT_FOUND(
            "Media not found",
            HttpStatus.NOT_FOUND
    ),

    MEDIA_NOT_VIDEO(
            "Media must be a video",
            HttpStatus.BAD_REQUEST
    ),

    UNSUPPORTED_VIDEO_TYPE(
            "Only MP4, MOV and WEBM videos are supported",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE
    ),

    VIDEO_TOO_LARGE(
            "Video is too large to analyze",
            HttpStatus.PAYLOAD_TOO_LARGE
    ),

    VIDEO_NOT_READABLE(
            "Video file cannot be read",
            HttpStatus.UNPROCESSABLE_ENTITY
    ),

    AI_UNAVAILABLE(
            "Video analysis is temporarily unavailable. Please try again later",
            HttpStatus.SERVICE_UNAVAILABLE
    ),

    AI_QUOTA_EXCEEDED(
            "Video analysis quota has been reached. Please try again later",
            HttpStatus.TOO_MANY_REQUESTS
    ),

    AI_TIMEOUT(
            "Video analysis timed out. Please try again or use a shorter video",
            HttpStatus.GATEWAY_TIMEOUT
    ),

    AI_INVALID_RESPONSE(
            "Video analysis returned an invalid result. Please try again",
            HttpStatus.BAD_GATEWAY
    ),

    AI_REFUSED(
            "This video could not be analyzed. Please use another video",
            HttpStatus.UNPROCESSABLE_ENTITY
    ),

    NOT_A_RECIPE(
            "This video does not show how to make a recipe",
            HttpStatus.UNPROCESSABLE_ENTITY
    );

    private final String message;

    private final HttpStatus httpStatus;
}
