package com.manhnv.capstonebakerai.recipe.excel;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.entity.Tool;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.recipe.excel.dto.ImportError;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedRecipe;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedRecipeIngredient;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedRecipeTool;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedStep;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedStepIngredient;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedStepTool;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.manhnv.capstonebakerai.recipe.excel.RecipeSheetLayout.*;

@Component
public class RecipeExcelParser {

    public ParseResult parse(Workbook workbook, RecipeImportMasterData masterData) {
        List<ImportError> errors = new ArrayList<>();
        List<ParsedRecipe> recipes = new ArrayList<>();

        List<Sheet> recipeSheets = new ArrayList<>();
        for (Sheet sheet : workbook) {
            if (!isSystemSheet(sheet.getSheetName())) {
                recipeSheets.add(sheet);
            }
        }
        if (recipeSheets.isEmpty()) {
            errors.add(ImportError.ofFile("File không có công thức nào để import"));
            return new ParseResult(recipes, errors);
        }
        if (recipeSheets.size() > MAX_RECIPE_SHEETS) {
            errors.add(ImportError.ofFile("File có " + recipeSheets.size() + " công thức, mỗi lần chỉ import tối đa "
                    + MAX_RECIPE_SHEETS + " công thức"));
            return new ParseResult(recipes, errors);
        }

        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();
        for (Sheet sheet : recipeSheets) {
            new SheetParser(sheet, masterData, evaluator, formatter, errors).parse().ifPresent(recipes::add);
        }
        return new ParseResult(recipes, errors);
    }

    public record ParseResult(List<ParsedRecipe> recipes, List<ImportError> errors) {
    }

    private static final class SheetParser {
        private final Sheet sheet;
        private final String sheetName;
        private final RecipeImportMasterData masterData;
        private final FormulaEvaluator evaluator;
        private final DataFormatter formatter;
        private final List<ImportError> errors;

        SheetParser(Sheet sheet, RecipeImportMasterData masterData, FormulaEvaluator evaluator,
                    DataFormatter formatter, List<ImportError> errors) {
            this.sheet = sheet;
            this.sheetName = sheet.getSheetName();
            this.masterData = masterData;
            this.evaluator = evaluator;
            this.formatter = formatter;
            this.errors = errors;
        }

        Optional<ParsedRecipe> parse() {
            int errorsBefore = errors.size();
            int stepsTitleRow = findLabelRow(STEPS_TITLE, 0, sheet.getLastRowNum());
            int infoLastRow = stepsTitleRow < 0 ? sheet.getLastRowNum() : stepsTitleRow - 1;

            int titleRow = findInfoRow(LABEL_TITLE, infoLastRow);
            String title = infoValue(titleRow);
            if (titleRow >= 0 && title == null) {
                error(titleRow + 1, VALUE_COLUMN, "Tên công thức không được để trống");
            }

            int descriptionRow = findLabelRow(LABEL_DESCRIPTION, 0, infoLastRow);
            String description = descriptionRow < 0 ? null : infoValue(descriptionRow);

            RecipeCategory category = parseCategory(findInfoRow(LABEL_CATEGORY, infoLastRow));
            DifficultyEnum difficulty = parseDifficulty(findInfoRow(LABEL_DIFFICULTY, infoLastRow));
            Integer servings = parseServings(findInfoRow(LABEL_SERVINGS, infoLastRow));

            List<ParsedStep> steps = List.of();
            if (stepsTitleRow < 0) {
                error(null, LABEL_COLUMN, "Không tìm thấy tiêu đề '" + STEPS_TITLE + "' ở cột A");
            } else {
                steps = parseSteps(stepsTitleRow);
            }

            List<ParsedRecipeIngredient> ingredients = aggregateIngredients(steps);
            List<ParsedRecipeTool> tools = aggregateTools(steps);

            if (errors.size() != errorsBefore) {
                return Optional.empty();
            }
            return Optional.of(new ParsedRecipe(sheetName, titleRow + 1, title, description, category, difficulty,
                    servings, steps, ingredients, tools,
                    sumDuration(steps, RecipeStepType.PREPARATION), sumDuration(steps, RecipeStepType.COOKING)));
        }

        private RecipeCategory parseCategory(int row) {
            String value = infoValue(row);
            if (row < 0) {
                return null;
            }
            if (value == null) {
                error(row + 1, VALUE_COLUMN, "Danh mục không được để trống");
                return null;
            }
            RecipeImportMasterData.Lookup<RecipeCategory> lookup = masterData.category(value);
            if (lookup.ambiguous()) {
                error(row + 1, VALUE_COLUMN, ambiguous(value));
            } else if (!lookup.found()) {
                error(row + 1, VALUE_COLUMN, "Danh mục '" + value + "' không tồn tại trong hệ thống");
            }
            return lookup.value();
        }

        private DifficultyEnum parseDifficulty(int row) {
            String value = infoValue(row);
            if (row < 0) {
                return null;
            }
            if (value == null) {
                error(row + 1, VALUE_COLUMN, "Độ khó không được để trống");
                return null;
            }
            DifficultyEnum difficulty = parseEnum(DifficultyEnum.class, value);
            if (difficulty == null) {
                error(row + 1, VALUE_COLUMN, "Độ khó '" + value + "' không hợp lệ. Giá trị hợp lệ: "
                        + enumValues(DifficultyEnum.class));
            }
            return difficulty;
        }

        private Integer parseServings(int row) {
            String value = infoValue(row);
            if (row < 0) {
                return null;
            }
            Integer servings = value == null ? null : parseInteger(value);
            if (servings == null || servings <= 0) {
                error(row + 1, VALUE_COLUMN, "Khẩu phần phải là số nguyên lớn hơn 0");
                return null;
            }
            return servings;
        }

        private List<ParsedStep> parseSteps(int stepsTitleRow) {
            List<StepDraft> drafts = new ArrayList<>();
            Set<Integer> seenNumbers = new HashSet<>();
            int lastNumber = 0;
            StepDraft current = null;

            for (int r = stepsTitleRow + 2; r <= sheet.getLastRowNum(); r++) {
                String[] values = rowValues(sheet.getRow(r));
                if (Arrays.stream(values).allMatch(Objects::isNull)) {
                    continue;
                }
                int rowNumber = r + 1;

                if (values[COL_STEP] != null) {
                    int expected = lastNumber + 1;
                    Integer number = parseInteger(values[COL_STEP]);
                    if (number == null || number <= 0) {
                        error(rowNumber, COL_STEP, "Số bước phải là số nguyên lớn hơn 0");
                        number = expected;
                    } else if (!seenNumbers.add(number)) {
                        error(rowNumber, COL_STEP, "Số bước " + number + " bị trùng");
                    } else if (number != expected) {
                        error(rowNumber, COL_STEP, "Số bước phải liên tục và tăng dần từ 1, dòng này phải là bước " + expected);
                    }
                    lastNumber = number;
                    current = new StepDraft(rowNumber, number);
                    drafts.add(current);
                    readStepInfo(current, values, rowNumber);
                } else {
                    if (current == null) {
                        error(rowNumber, COL_STEP, "Dòng phụ không thuộc bước nào, hãy điền số bước ở cột A");
                        continue;
                    }
                    if (values[COL_INSTRUCTION] != null || values[COL_DURATION] != null
                            || values[COL_STEP_TYPE] != null || values[COL_STEP_NOTE] != null) {
                        error(rowNumber, COL_STEP, "Thiếu số bước ở cột A");
                    }
                }
                readIngredient(current, values, rowNumber);
                readTool(current, values, rowNumber);
            }

            if (drafts.isEmpty()) {
                error(stepsTitleRow + 2, LABEL_COLUMN, "Công thức phải có ít nhất 1 bước");
            }
            return drafts.stream().map(StepDraft::toParsed).toList();
        }

        private void readStepInfo(StepDraft step, String[] values, int rowNumber) {
            step.instruction = values[COL_INSTRUCTION];
            if (step.instruction == null) {
                error(rowNumber, COL_INSTRUCTION, "Thiếu hướng dẫn của bước");
            }
            if (values[COL_DURATION] != null) {
                Integer duration = parseInteger(values[COL_DURATION]);
                if (duration == null || duration < 0) {
                    error(rowNumber, COL_DURATION, "Thời gian (phút) phải là số nguyên lớn hơn hoặc bằng 0");
                } else {
                    step.durationMinutes = duration;
                }
            }
            String type = values[COL_STEP_TYPE];
            if (type == null) {
                error(rowNumber, COL_STEP_TYPE, "Thiếu loại bước");
            } else {
                step.type = parseEnum(RecipeStepType.class, type);
                if (step.type == null) {
                    error(rowNumber, COL_STEP_TYPE, "Loại bước '" + type + "' không hợp lệ. Giá trị hợp lệ: "
                            + enumValues(RecipeStepType.class));
                }
            }
            step.notes = values[COL_STEP_NOTE];
        }

        private void readIngredient(StepDraft step, String[] values, int rowNumber) {
            String name = values[COL_INGREDIENT];
            String quantityText = values[COL_INGREDIENT_QUANTITY];
            String unit = values[COL_INGREDIENT_UNIT];
            String note = values[COL_INGREDIENT_NOTE];
            if (name == null) {
                if (quantityText != null || unit != null || note != null) {
                    error(rowNumber, COL_INGREDIENT, "Thiếu tên nguyên liệu");
                }
                return;
            }
            boolean valid = true;
            RecipeImportMasterData.Lookup<Ingredient> lookup = masterData.ingredient(name);
            if (lookup.ambiguous()) {
                error(rowNumber, COL_INGREDIENT, ambiguous(name));
                valid = false;
            } else if (!lookup.found()) {
                error(rowNumber, COL_INGREDIENT, "Nguyên liệu '" + name + "' không tồn tại trong hệ thống");
                valid = false;
            }
            BigDecimal quantity = null;
            if (quantityText == null) {
                error(rowNumber, COL_INGREDIENT_QUANTITY, "Thiếu số lượng nguyên liệu");
                valid = false;
            } else {
                quantity = parseDecimal(quantityText);
                if (quantity == null || quantity.signum() <= 0) {
                    error(rowNumber, COL_INGREDIENT_QUANTITY, "Số lượng nguyên liệu phải là số lớn hơn 0");
                    valid = false;
                }
            }
            if (unit == null) {
                error(rowNumber, COL_INGREDIENT_UNIT, "Thiếu đơn vị của nguyên liệu");
                valid = false;
            }
            if (!valid) {
                return;
            }
            Ingredient ingredient = lookup.value();
            if (!step.ingredientIds.add(ingredient.getId())) {
                error(rowNumber, COL_INGREDIENT, "Nguyên liệu '" + ingredient.getName() + "' bị lặp lại trong bước "
                        + step.stepNumber);
                return;
            }
            step.ingredients.add(new ParsedStepIngredient(rowNumber, ingredient, quantity, unit, note));
        }

        private void readTool(StepDraft step, String[] values, int rowNumber) {
            String name = values[COL_TOOL];
            String quantityText = values[COL_TOOL_QUANTITY];
            String note = values[COL_TOOL_NOTE];
            if (name == null) {
                if (quantityText != null || note != null) {
                    error(rowNumber, COL_TOOL, "Thiếu tên dụng cụ");
                }
                return;
            }
            boolean valid = true;
            RecipeImportMasterData.Lookup<Tool> lookup = masterData.tool(name);
            if (lookup.ambiguous()) {
                error(rowNumber, COL_TOOL, ambiguous(name));
                valid = false;
            } else if (!lookup.found()) {
                error(rowNumber, COL_TOOL, "Dụng cụ '" + name + "' không tồn tại trong hệ thống");
                valid = false;
            }
            BigDecimal quantity = null;
            if (quantityText == null) {
                error(rowNumber, COL_TOOL_QUANTITY, "Thiếu số lượng dụng cụ");
                valid = false;
            } else {
                quantity = parseDecimal(quantityText);
                if (quantity == null || quantity.signum() <= 0) {
                    error(rowNumber, COL_TOOL_QUANTITY, "Số lượng dụng cụ phải là số lớn hơn 0");
                    valid = false;
                }
            }
            if (!valid) {
                return;
            }
            Tool tool = lookup.value();
            if (!step.toolIds.add(tool.getId())) {
                error(rowNumber, COL_TOOL, "Dụng cụ '" + tool.getName() + "' bị lặp lại trong bước " + step.stepNumber);
                return;
            }
            step.tools.add(new ParsedStepTool(rowNumber, tool, quantity, note));
        }

        private List<ParsedRecipeIngredient> aggregateIngredients(List<ParsedStep> steps) {
            Map<UUID, List<ParsedStepIngredient>> byIngredient = new LinkedHashMap<>();
            for (ParsedStep step : steps) {
                for (ParsedStepIngredient item : step.ingredients()) {
                    byIngredient.computeIfAbsent(item.ingredient().getId(), id -> new ArrayList<>()).add(item);
                }
            }
            List<ParsedRecipeIngredient> result = new ArrayList<>();
            for (List<ParsedStepIngredient> items : byIngredient.values()) {
                Map<String, List<ParsedStepIngredient>> byUnit = items.stream().collect(Collectors.groupingBy(
                        item -> item.unit().toLowerCase(Locale.ROOT), LinkedHashMap::new, Collectors.toList()));
                Ingredient ingredient = items.get(0).ingredient();
                if (byUnit.size() > 1) {
                    String detail = byUnit.values().stream()
                            .map(group -> group.get(0).unit() + " ở dòng " + group.stream()
                                    .map(item -> String.valueOf(item.rowNumber())).collect(Collectors.joining(", ")))
                            .collect(Collectors.joining(", "));
                    error(items.get(0).rowNumber(), COL_INGREDIENT_UNIT, "Nguyên liệu '" + ingredient.getName()
                            + "' dùng nhiều đơn vị khác nhau (" + detail + "). Hãy dùng thống nhất một đơn vị");
                    continue;
                }
                BigDecimal total = items.stream().map(ParsedStepIngredient::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                result.add(new ParsedRecipeIngredient(ingredient, total, items.get(0).unit()));
            }
            return result;
        }

        private List<ParsedRecipeTool> aggregateTools(List<ParsedStep> steps) {
            Map<UUID, ParsedRecipeTool> byTool = new LinkedHashMap<>();
            for (ParsedStep step : steps) {
                for (ParsedStepTool item : step.tools()) {
                    byTool.merge(item.tool().getId(), new ParsedRecipeTool(item.tool(), item.quantity()),
                            (a, b) -> a.quantity().compareTo(b.quantity()) >= 0 ? a : b);
                }
            }
            return new ArrayList<>(byTool.values());
        }

        private static Integer sumDuration(List<ParsedStep> steps, RecipeStepType type) {
            List<Integer> durations = steps.stream()
                    .filter(step -> step.recipeStepType() == type)
                    .map(ParsedStep::durationMinutes)
                    .filter(Objects::nonNull)
                    .toList();
            return durations.isEmpty() ? null : durations.stream().mapToInt(Integer::intValue).sum();
        }

        private int findInfoRow(String label, int lastRow) {
            int row = findLabelRow(label, 0, lastRow);
            if (row < 0) {
                error(null, LABEL_COLUMN, "Không tìm thấy nhãn '" + label + "' ở cột A");
            }
            return row;
        }

        private String infoValue(int row) {
            return row < 0 ? null : text(sheet.getRow(row), VALUE_COLUMN);
        }

        private int findLabelRow(String label, int fromRow, int toRow) {
            String expected = normalizeLabel(label);
            for (int r = Math.max(fromRow, 0); r <= toRow; r++) {
                String value = text(sheet.getRow(r), LABEL_COLUMN);
                if (value != null && normalizeLabel(value).equals(expected)) {
                    return r;
                }
            }
            return -1;
        }

        private String[] rowValues(Row row) {
            String[] values = new String[STEP_COLUMN_COUNT];
            for (int c = 0; c < STEP_COLUMN_COUNT; c++) {
                values[c] = text(row, c);
            }
            return values;
        }

        private String text(Row row, int column) {
            if (row == null) {
                return null;
            }
            Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                return null;
            }
            String value;
            try {
                CellType type = cell.getCellType() == CellType.FORMULA
                        ? evaluator.evaluateFormulaCell(cell)
                        : cell.getCellType();
                if (type == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
                    value = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
                } else {
                    value = formatter.formatCellValue(cell, evaluator);
                }
            } catch (RuntimeException exception) {
                value = formatter.formatCellValue(cell);
            }
            value = value.strip();
            return value.isEmpty() ? null : value;
        }

        private void error(Integer rowNumber, Integer column, String message) {
            errors.add(new ImportError(sheetName, rowNumber,
                    column == null ? null : CellReference.convertNumToColString(column), message));
        }

        private static String ambiguous(String name) {
            return "Tên '" + name + "' khớp nhiều bản ghi trong hệ thống, hãy liên hệ quản trị";
        }
    }

    private static final class StepDraft {
        private final int rowNumber;
        private final int stepNumber;
        private String instruction;
        private Integer durationMinutes;
        private RecipeStepType type;
        private String notes;
        private final List<ParsedStepIngredient> ingredients = new ArrayList<>();
        private final List<ParsedStepTool> tools = new ArrayList<>();
        private final Set<UUID> ingredientIds = new HashSet<>();
        private final Set<UUID> toolIds = new HashSet<>();

        StepDraft(int rowNumber, int stepNumber) {
            this.rowNumber = rowNumber;
            this.stepNumber = stepNumber;
        }

        ParsedStep toParsed() {
            return new ParsedStep(rowNumber, stepNumber, instruction, durationMinutes, type, notes,
                    List.copyOf(ingredients), List.copyOf(tools));
        }
    }

    static String normalizeLabel(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    static BigDecimal parseDecimal(String text) {
        String value = text.strip().replace(" ", "");
        if (value.contains(",") && value.contains(".")) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static Integer parseInteger(String text) {
        BigDecimal value = parseDecimal(text);
        if (value == null) {
            return null;
        }
        try {
            return value.stripTrailingZeros().intValueExact();
        } catch (ArithmeticException exception) {
            return null;
        }
    }

    static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    static <E extends Enum<E>> String enumValues(Class<E> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
