package com.manhnv.capstonebakerai.recipe.excel;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.controller.RecipeExcelController;
import com.manhnv.capstonebakerai.recipe.excel.dto.ImportError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = RecipeExcelController.class)
public class RecipeExcelExceptionHandler {

    @ExceptionHandler(ExcelImportValidationException.class)
    public ResponseEntity<ApiResponse<List<ImportError>>> handleExcelImportValidation(
            ExcelImportValidationException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<List<ImportError>>builder()
                        .success(false)
                        .message(exception.getMessage())
                        .data(exception.getErrors())
                        .build());
    }
}
