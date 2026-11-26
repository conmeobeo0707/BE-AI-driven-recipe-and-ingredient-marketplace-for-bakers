package com.manhnv.capstonebakerai.recipe.excel.dto;

public record ImportError(String sheetName, Integer rowNumber, String column, String message) {

    public static ImportError ofFile(String message) {
        return new ImportError(null, null, null, message);
    }
}
