package com.manhnv.capstonebakerai.recipe.excel;

import com.manhnv.capstonebakerai.recipe.excel.dto.ImportError;
import lombok.Getter;

import java.util.List;

@Getter
public class ExcelImportValidationException extends RuntimeException {

    private final List<ImportError> errors;

    public ExcelImportValidationException(List<ImportError> errors) {
        super("File có " + errors.size() + " lỗi, chưa có dữ liệu nào được lưu");
        this.errors = List.copyOf(errors);
    }
}
