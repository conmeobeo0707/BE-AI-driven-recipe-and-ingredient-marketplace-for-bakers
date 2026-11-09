package com.manhnv.capstonebakerai.recipe.suggestion.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecipeSuggestionErrorCode implements ErrorCode {

    INVALID_SUGGESTION_LIMIT(
            "Limit must be between 1 and 5",
            HttpStatus.BAD_REQUEST
    ),
    INVALID_CATALOG_SELECTION(
            "Select between 1 and 20 existing, active catalog ingredients",
            HttpStatus.BAD_REQUEST
    );

    private final String message;
    private final HttpStatus httpStatus;
}
