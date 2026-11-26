package com.manhnv.capstonebakerai.recipe.exception;

import com.manhnv.capstonebakerai.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecipeCatalogErrorCode implements ErrorCode {

    NAME_REQUIRED("Tên không được để trống", HttpStatus.BAD_REQUEST),
    TOOL_NAME_ALREADY_EXISTS("Dụng cụ đã tồn tại", HttpStatus.CONFLICT),
    CATEGORY_NAME_ALREADY_EXISTS("Danh mục đã tồn tại", HttpStatus.CONFLICT);

    private final String message;
    private final HttpStatus httpStatus;
}
