package com.manhnv.capstonebakerai.recipe.ingredientalias.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IngredientAliasErrorCode implements ErrorCode {

    INVALID_INGREDIENT_NAME(
            "Ingredient name must contain letters or numbers and be at most 150 characters",
            HttpStatus.BAD_REQUEST
    ),
    INGREDIENT_NOT_FOUND("Ingredient not found", HttpStatus.NOT_FOUND),
    ALIAS_NOT_FOUND("Ingredient alias not found", HttpStatus.NOT_FOUND),
    ALIAS_ALREADY_EXISTS("This alias already exists for the ingredient", HttpStatus.CONFLICT),
    NAME_ALREADY_IN_USE("This name already belongs to another ingredient", HttpStatus.CONFLICT),
    DUPLICATE_NAMES_IN_REQUEST("The same name appears more than once in the request", HttpStatus.CONFLICT),
    INVALID_SEARCH("Keyword must be at most 150 characters and limit must be between 1 and 20", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;
}
