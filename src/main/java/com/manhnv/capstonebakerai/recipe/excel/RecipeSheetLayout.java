package com.manhnv.capstonebakerai.recipe.excel;

import java.util.List;
import java.util.Set;

public final class RecipeSheetLayout {

    public static final String SHEET_GUIDE = "HuongDan";
    public static final String SHEET_TEMPLATE = "MAU";
    public static final String SHEET_MASTER_DATA = "MasterData";
    public static final Set<String> SYSTEM_SHEETS = Set.of(
            SHEET_GUIDE.toLowerCase(), SHEET_TEMPLATE.toLowerCase(), SHEET_MASTER_DATA.toLowerCase());

    public static final String RANGE_CATEGORIES = "Categories";
    public static final String RANGE_INGREDIENTS = "Ingredients";
    public static final String RANGE_TOOLS = "Tools";
    public static final String RANGE_DIFFICULTY = "Difficulty";
    public static final String RANGE_STEP_TYPE = "RecipeStepType";

    public static final String INFO_TITLE = "THÔNG TIN";
    public static final String LABEL_TITLE = "Tên công thức";
    public static final String LABEL_DESCRIPTION = "Mô tả";
    public static final String LABEL_CATEGORY = "Danh mục";
    public static final String LABEL_DIFFICULTY = "Độ khó";
    public static final String LABEL_SERVINGS = "Khẩu phần";

    public static final int INFO_TITLE_ROW = 0;
    public static final int TITLE_ROW = 1;
    public static final int DESCRIPTION_ROW = 2;
    public static final int CATEGORY_ROW = 3;
    public static final int DIFFICULTY_ROW = 4;
    public static final int SERVINGS_ROW = 5;
    public static final int LABEL_COLUMN = 0;
    public static final int VALUE_COLUMN = 1;

    public static final String STEPS_TITLE = "CÁC BƯỚC";
    public static final int STEPS_TITLE_ROW = 7;
    public static final int STEP_HEADER_ROW = 8;
    public static final int STEP_FIRST_DATA_ROW = 9;
    public static final int STEP_INPUT_ROWS = 200;
    public static final int STEP_LAST_INPUT_ROW = STEP_FIRST_DATA_ROW + STEP_INPUT_ROWS - 1;

    public static final int COL_STEP = 0;
    public static final int COL_INSTRUCTION = 1;
    public static final int COL_DURATION = 2;
    public static final int COL_STEP_TYPE = 3;
    public static final int COL_STEP_NOTE = 4;
    public static final int COL_INGREDIENT = 5;
    public static final int COL_INGREDIENT_QUANTITY = 6;
    public static final int COL_INGREDIENT_UNIT = 7;
    public static final int COL_INGREDIENT_NOTE = 8;
    public static final int COL_TOOL = 9;
    public static final int COL_TOOL_QUANTITY = 10;
    public static final int COL_TOOL_NOTE = 11;
    public static final int STEP_COLUMN_COUNT = 12;

    public static final List<String> STEP_HEADERS = List.of(
            "Bước", "Hướng dẫn", "Thời gian (phút)", "Loại bước", "Ghi chú bước",
            "Nguyên liệu", "SL nguyên liệu", "Đơn vị", "Ghi chú NL",
            "Dụng cụ", "SL dụng cụ", "Ghi chú DC");

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_RECIPE_SHEETS = 50;

    private RecipeSheetLayout() {
    }

    public static boolean isSystemSheet(String sheetName) {
        return sheetName != null && SYSTEM_SHEETS.contains(sheetName.strip().toLowerCase());
    }
}
