package com.manhnv.capstonebakerai.recipe.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecipeCatalogErrorCode implements ErrorCode {

    NAME_REQUIRED("Name is required", HttpStatus.BAD_REQUEST),
    TOOL_NAME_ALREADY_EXISTS("Tool already exists", HttpStatus.CONFLICT),
    CATEGORY_NAME_ALREADY_EXISTS("Category already exists", HttpStatus.CONFLICT),
    TOOL_NOT_FOUND("Tool not found", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("Recipe category not found", HttpStatus.NOT_FOUND);

    private final String message;
    private final HttpStatus httpStatus;
}
