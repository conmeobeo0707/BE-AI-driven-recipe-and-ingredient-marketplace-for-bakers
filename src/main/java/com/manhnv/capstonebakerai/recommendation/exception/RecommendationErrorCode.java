package com.manhnv.capstonebakerai.recommendation.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecommendationErrorCode implements ErrorCode {

    INVALID_K("k must be between 1 and 20", HttpStatus.BAD_REQUEST),
    NOT_ENOUGH_TRAINING_DATA("Not enough interaction data to train a model", HttpStatus.BAD_REQUEST),
    TRAINING_FAILED("Training did not produce usable weights; the current weights are kept", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String message;
    private final HttpStatus httpStatus;
}
