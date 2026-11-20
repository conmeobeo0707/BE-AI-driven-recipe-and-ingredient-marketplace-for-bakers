package com.manhnv.capstonebakerai.recipe.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecipeInteractionErrorCode implements ErrorCode {

    RECIPE_NOT_FOUND(
            "Recipe not found or not published",
            HttpStatus.NOT_FOUND
    ),
    CUSTOMER_NOT_FOUND(
            "Customer profile not found",
            HttpStatus.NOT_FOUND
    );

    private final String message;
    private final HttpStatus httpStatus;
}
