package com.manhnv.capstonebakerai.recipe.excel;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.entity.Tool;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderFormatting;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.manhnv.capstonebakerai.recipe.excel.RecipeSheetLayout.*;

@Service
@RequiredArgsConstructor
public class RecipeExcelTemplateService {

    private static final List<String> GUIDE_LINES = List.of(
            "1. Mỗi món = 1 sheet. Chuột phải sheet 'MAU' → Move or Copy → tick Create a copy, rồi đổi tên sheet theo tên món.",
            "2. Điền phần THÔNG TIN (dòng 1–6), sau đó điền bảng CÁC BƯỚC (từ dòng 10).",
            "3. Một bước dùng nhiều nguyên liệu/dụng cụ: ghi tiếp ở các dòng ngay bên dưới, để trống cột 'Bước' và các cột B–E.",
            "4. Số bước phải liên tục và tăng dần: 1, 2, 3...",
            "5. Mọi nguyên liệu phải nằm trong một bước. Hệ thống tự cộng danh sách nguyên liệu tổng từ các bước.",
            "6. Một nguyên liệu phải dùng cùng một đơn vị trong cả công thức (ví dụ luôn là 'gram').",
            "7. Nguyên liệu, dụng cụ bắt buộc có số lượng. Số thập phân có thể ghi 0.5 hoặc 0,5.",
            "8. Có thể gõ tên gọi khác (alias) của nguyên liệu nếu không thấy trong danh sách chọn.",
            "9. Không sửa vị trí các ô tiêu đề (THÔNG TIN, CÁC BƯỚC, dòng tiêu đề bảng) và các nhãn ở cột A.",
            "10. Tên công thức không được trùng giữa các sheet và không trùng công thức bạn đã có trên hệ thống.",
            "11. Không cần xóa sheet 'MAU', hệ thống tự bỏ qua. Mỗi file tối đa "
                    + MAX_RECIPE_SHEETS + " công thức, dung lượng tối đa 5MB.",
            "12. Công thức sau khi import ở trạng thái DRAFT, hãy kiểm tra lại trên web trước khi publish.");

    private static final int[] STEP_COLUMN_WIDTHS = {16, 48, 12, 15, 26, 26, 12, 10, 22, 24, 11, 22};

    private final RecipeCategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final ToolRepository toolRepository;

    @Transactional(readOnly = true)
    public byte[] buildTemplate() {
        return build(
                categoryRepository.findAllByOrderByNameAscIdAsc().stream().map(RecipeCategory::getName).toList(),
                ingredientRepository.findAllByOrderByNameAscIdAsc().stream().map(Ingredient::getName).toList(),
                toolRepository.findAllByOrderByNameAscIdAsc().stream().map(Tool::getName).toList());
    }

    public byte[] build(List<String> categories, List<String> ingredients, List<String> tools) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            XSSFSheet guide = workbook.createSheet(SHEET_GUIDE);
            XSSFSheet template = workbook.createSheet(SHEET_TEMPLATE);
            XSSFSheet masterData = workbook.createSheet(SHEET_MASTER_DATA);

            buildGuide(guide, styles);
            buildMasterData(workbook, masterData, styles, categories, ingredients, tools);
            buildRecipeSheet(template, styles, categories, ingredients, tools);

            int masterIndex = workbook.getSheetIndex(masterData);
            masterData.protectSheet(UUID.randomUUID().toString());
            workbook.setSheetHidden(masterIndex, true);
            int templateIndex = workbook.getSheetIndex(template);
            workbook.setActiveSheet(templateIndex);
            guide.setSelected(false);
            template.setSelected(true);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void buildGuide(XSSFSheet sheet, Styles styles) {
        sheet.setColumnWidth(0, 125 * 256);
        Cell title = sheet.createRow(0).createCell(0);
        title.setCellValue("HƯỚNG DẪN IMPORT CÔNG THỨC");
        title.setCellStyle(styles.title);
        for (int i = 0; i < GUIDE_LINES.size(); i++) {
            Cell cell = sheet.createRow(i + 2).createCell(0);
            cell.setCellValue(GUIDE_LINES.get(i));
            cell.setCellStyle(styles.wrap);
        }
    }

    private static void buildMasterData(XSSFWorkbook workbook, XSSFSheet sheet, Styles styles, List<String> categories,
                                        List<String> ingredients, List<String> tools) {
        addMasterColumn(workbook, sheet, styles, 0, RANGE_CATEGORIES, categories);
        addMasterColumn(workbook, sheet, styles, 1, RANGE_INGREDIENTS, ingredients);
        addMasterColumn(workbook, sheet, styles, 2, RANGE_TOOLS, tools);
        addMasterColumn(workbook, sheet, styles, 3, RANGE_DIFFICULTY,
                Arrays.stream(DifficultyEnum.values()).map(Enum::name).toList());
        addMasterColumn(workbook, sheet, styles, 4, RANGE_STEP_TYPE,
                Arrays.stream(RecipeStepType.values()).map(Enum::name).toList());
    }

    private static void addMasterColumn(XSSFWorkbook workbook, XSSFSheet sheet, Styles styles, int column,
                                        String rangeName, List<String> values) {
        Cell header = row(sheet, 0).createCell(column);
        header.setCellValue(rangeName);
        header.setCellStyle(styles.header);
        for (int i = 0; i < values.size(); i++) {
            row(sheet, i + 1).createCell(column).setCellValue(values.get(i));
        }
        sheet.setColumnWidth(column, 32 * 256);
        String letter = CellReference.convertNumToColString(column);
        int lastRow = Math.max(values.size(), 1) + 1;
        Name name = workbook.createName();
        name.setNameName(rangeName);
        name.setRefersToFormula("'" + SHEET_MASTER_DATA + "'!$" + letter + "$2:$" + letter + "$" + lastRow);
    }

    private static void buildRecipeSheet(XSSFSheet sheet, Styles styles, List<String> categories,
                                         List<String> ingredients, List<String> tools) {
        for (int c = 0; c < STEP_COLUMN_WIDTHS.length; c++) {
            sheet.setColumnWidth(c, STEP_COLUMN_WIDTHS[c] * 256);
        }

        Cell infoTitle = row(sheet, INFO_TITLE_ROW).createCell(LABEL_COLUMN);
        infoTitle.setCellValue(INFO_TITLE);
        infoTitle.setCellStyle(styles.title);
        infoRow(sheet, styles, TITLE_ROW, LABEL_TITLE, "Bánh mì bơ tỏi");
        infoRow(sheet, styles, DESCRIPTION_ROW, LABEL_DESCRIPTION, "Công thức mẫu minh họa cách nhập liệu");
        infoRow(sheet, styles, CATEGORY_ROW, LABEL_CATEGORY, first(categories, 0));
        infoRow(sheet, styles, DIFFICULTY_ROW, LABEL_DIFFICULTY, DifficultyEnum.EASY.name());
        Cell servingsLabel = row(sheet, SERVINGS_ROW).createCell(LABEL_COLUMN);
        servingsLabel.setCellValue(LABEL_SERVINGS);
        servingsLabel.setCellStyle(styles.label);
        row(sheet, SERVINGS_ROW).createCell(VALUE_COLUMN).setCellValue(4);

        listValidation(sheet, RANGE_CATEGORIES, new CellRangeAddressList(CATEGORY_ROW, CATEGORY_ROW, VALUE_COLUMN, VALUE_COLUMN), true);
        listValidation(sheet, RANGE_DIFFICULTY, new CellRangeAddressList(DIFFICULTY_ROW, DIFFICULTY_ROW, VALUE_COLUMN, VALUE_COLUMN), true);
        numberValidation(sheet, true, DataValidationConstraint.OperatorType.GREATER_THAN,
                new CellRangeAddressList(SERVINGS_ROW, SERVINGS_ROW, VALUE_COLUMN, VALUE_COLUMN),
                "Khẩu phần phải là số nguyên lớn hơn 0");

        Cell stepsTitle = row(sheet, STEPS_TITLE_ROW).createCell(LABEL_COLUMN);
        stepsTitle.setCellValue(STEPS_TITLE);
        stepsTitle.setCellStyle(styles.title);
        Row header = row(sheet, STEP_HEADER_ROW);
        for (int c = 0; c < STEP_HEADERS.size(); c++) {
            Cell cell = header.createCell(c);
            cell.setCellValue(STEP_HEADERS.get(c));
            cell.setCellStyle(styles.header);
        }

        String ingredient1 = first(ingredients, 0);
        String ingredient2 = first(ingredients, 1);
        String tool1 = first(tools, 0);
        String tool2 = first(tools, 1);
        Row step1 = row(sheet, STEP_FIRST_DATA_ROW);
        set(step1, COL_STEP, 1);
        set(step1, COL_INSTRUCTION, "Trộn đều bột và men nở trong âu lớn");
        set(step1, COL_DURATION, 15);
        set(step1, COL_STEP_TYPE, RecipeStepType.PREPARATION.name());
        set(step1, COL_STEP_NOTE, "Nhào tới khi mịn");
        if (ingredient1 != null) {
            set(step1, COL_INGREDIENT, ingredient1);
            set(step1, COL_INGREDIENT_QUANTITY, 300);
            set(step1, COL_INGREDIENT_UNIT, "gram");
            set(step1, COL_INGREDIENT_NOTE, "Rây mịn");
        }
        if (tool1 != null) {
            set(step1, COL_TOOL, tool1);
            set(step1, COL_TOOL_QUANTITY, 1);
        }
        if (ingredient2 != null) {
            Row extra = row(sheet, STEP_FIRST_DATA_ROW + 1);
            set(extra, COL_INGREDIENT, ingredient2);
            set(extra, COL_INGREDIENT_QUANTITY, 5);
            set(extra, COL_INGREDIENT_UNIT, "gram");
        }
        Row step2 = row(sheet, STEP_FIRST_DATA_ROW + 2);
        set(step2, COL_STEP, 2);
        set(step2, COL_INSTRUCTION, "Nướng ở 180°C");
        set(step2, COL_DURATION, 20);
        set(step2, COL_STEP_TYPE, RecipeStepType.COOKING.name());
        if (tool2 != null) {
            set(step2, COL_TOOL, tool2);
            set(step2, COL_TOOL_QUANTITY, 1);
            set(step2, COL_TOOL_NOTE, "Làm nóng trước");
        }
        for (int r = STEP_FIRST_DATA_ROW; r <= STEP_FIRST_DATA_ROW + 2; r++) {
            Cell instruction = row(sheet, r).getCell(COL_INSTRUCTION);
            if (instruction != null) {
                instruction.setCellStyle(styles.wrap);
            }
        }

        listValidation(sheet, RANGE_STEP_TYPE, stepColumn(COL_STEP_TYPE), true);
        listValidation(sheet, RANGE_INGREDIENTS, stepColumn(COL_INGREDIENT), false);
        listValidation(sheet, RANGE_TOOLS, stepColumn(COL_TOOL), true);
        numberValidation(sheet, true, DataValidationConstraint.OperatorType.GREATER_THAN, stepColumn(COL_STEP),
                "Số bước phải là số nguyên lớn hơn 0");
        numberValidation(sheet, true, DataValidationConstraint.OperatorType.GREATER_OR_EQUAL, stepColumn(COL_DURATION),
                "Thời gian phải là số nguyên lớn hơn hoặc bằng 0");
        numberValidation(sheet, false, DataValidationConstraint.OperatorType.GREATER_THAN,
                stepColumn(COL_INGREDIENT_QUANTITY), "Số lượng phải lớn hơn 0");
        numberValidation(sheet, false, DataValidationConstraint.OperatorType.GREATER_THAN,
                stepColumn(COL_TOOL_QUANTITY), "Số lượng phải lớn hơn 0");

        highlightStepRows(sheet);
        sheet.createFreezePane(0, STEP_FIRST_DATA_ROW);
    }

    private static void highlightStepRows(XSSFSheet sheet) {
        SheetConditionalFormatting formatting = sheet.getSheetConditionalFormatting();
        String firstCell = "$A" + (STEP_FIRST_DATA_ROW + 1);
        ConditionalFormattingRule rule = formatting.createConditionalFormattingRule("LEN(TRIM(" + firstCell + "))>0");
        PatternFormatting fill = rule.createPatternFormatting();
        fill.setFillBackgroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        BorderFormatting border = rule.createBorderFormatting();
        border.setBorderTop(BorderStyle.MEDIUM);
        border.setTopBorderColor(IndexedColors.GREY_50_PERCENT.getIndex());
        formatting.addConditionalFormatting(new CellRangeAddress[]{
                new CellRangeAddress(STEP_FIRST_DATA_ROW, STEP_LAST_INPUT_ROW, 0, STEP_COLUMN_COUNT - 1)}, rule);
    }

    private static CellRangeAddressList stepColumn(int column) {
        return new CellRangeAddressList(STEP_FIRST_DATA_ROW, STEP_LAST_INPUT_ROW, column, column);
    }

    private static void listValidation(Sheet sheet, String rangeName, CellRangeAddressList range, boolean strict) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidation validation = helper.createValidation(helper.createFormulaListConstraint(rangeName), range);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(strict ? DataValidation.ErrorStyle.STOP : DataValidation.ErrorStyle.WARNING);
        validation.createErrorBox("Giá trị không có trong danh sách", strict
                ? "Hãy chọn một giá trị trong danh sách"
                : "Giá trị không có trong danh sách. Bạn vẫn có thể giữ nếu đây là tên gọi khác của nguyên liệu");
        sheet.addValidationData(validation);
    }

    private static void numberValidation(Sheet sheet, boolean integer, int operator, CellRangeAddressList range,
                                         String message) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = integer
                ? helper.createIntegerConstraint(operator, "0", null)
                : helper.createDecimalConstraint(operator, "0", null);
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("Giá trị không hợp lệ", message);
        sheet.addValidationData(validation);
    }

    private static void infoRow(Sheet sheet, Styles styles, int rowIndex, String label, String value) {
        Row row = row(sheet, rowIndex);
        Cell labelCell = row.createCell(LABEL_COLUMN);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.label);
        if (value != null) {
            row.createCell(VALUE_COLUMN).setCellValue(value);
        }
    }

    private static void set(Row row, int column, String value) {
        row.createCell(column).setCellValue(value);
    }

    private static void set(Row row, int column, double value) {
        row.createCell(column).setCellValue(value);
    }

    private static Row row(Sheet sheet, int index) {
        Row row = sheet.getRow(index);
        return row != null ? row : sheet.createRow(index);
    }

    private static String first(List<String> values, int index) {
        if (values.isEmpty()) {
            return null;
        }
        return values.size() > index ? values.get(index) : values.get(0);
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle header;
        private final CellStyle label;
        private final CellStyle wrap;

        Styles(XSSFWorkbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            title = workbook.createCellStyle();
            title.setFont(titleFont);

            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(boldFont);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setBorderBottom(BorderStyle.THIN);
            header.setBorderTop(BorderStyle.THIN);
            header.setBorderLeft(BorderStyle.THIN);
            header.setBorderRight(BorderStyle.THIN);
            header.setWrapText(true);
            header.setVerticalAlignment(VerticalAlignment.CENTER);

            label = workbook.createCellStyle();
            label.setFont(boldFont);

            wrap = workbook.createCellStyle();
            wrap.setWrapText(true);
            wrap.setVerticalAlignment(VerticalAlignment.TOP);
        }
    }
}
