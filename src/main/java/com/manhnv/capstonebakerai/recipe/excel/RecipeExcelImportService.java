package com.manhnv.capstonebakerai.recipe.excel;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.ToolEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.excel.dto.ImportError;
import com.manhnv.capstonebakerai.recipe.excel.dto.ImportResultResponse;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedRecipe;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedRecipeIngredient;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedRecipeTool;
import com.manhnv.capstonebakerai.recipe.excel.dto.ParsedStep;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeMapper;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;
import com.manhnv.capstonebakerai.user.service.UserSummaryService;
import com.manhnv.capstonebakerai.recipe.ingredientalias.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientNormalizer;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeExcelImportService {

    private final RecipeExcelParser parser;
    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
    private final RecipeCategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;
    private final ToolRepository toolRepository;
    private final IngredientNormalizer normalizer;
    private final UserSummaryService userSummaryService;

    @Transactional
    public ImportResultResponse importRecipes(MultipartFile file, boolean dryRun) {
        validateFile(file);
        UUID userId = SecurityUtils.getCurrentUserId();

        RecipeImportMasterData masterData = RecipeImportMasterData.of(categoryRepository.findAll(),
                ingredientRepository.findAll(), aliasRepository.findAll(), toolRepository.findAll(), normalizer);

        RecipeExcelParser.ParseResult result;
        try (Workbook workbook = openWorkbook(file)) {
            result = parser.parse(workbook, masterData);
        } catch (IOException exception) {
            throw invalidFormat();
        }

        List<ImportError> errors = new ArrayList<>(result.errors());
        validateTitles(result.recipes(), userId, errors);
        if (!errors.isEmpty()) {
            throw new ExcelImportValidationException(errors);
        }

        List<RecipeResponse> recipes = new ArrayList<>();
        if (dryRun) {
            UserSummaryResponse user = userSummaryService.getSummary(userId);
            for (ParsedRecipe recipe : result.recipes()) {
                recipes.add(preview(recipe, userId, user));
            }
        } else {
            for (ParsedRecipe recipe : result.recipes()) {
                recipes.add(save(recipe));
            }
        }
        return new ImportResultResponse(dryRun, recipes.size(), recipes);
    }

    private RecipeResponse save(ParsedRecipe recipe) {
        try {
            return recipeService.create(toRequest(recipe));
        } catch (RuntimeException exception) {
            log.warn("Excel import failed while saving sheet '{}'", recipe.sheetName(), exception);
            throw new ExcelImportValidationException(List.of(new ImportError(recipe.sheetName(),
                    recipe.titleRowNumber(), null, "Không lưu được công thức: " + reason(exception))));
        }
    }

    private static String reason(RuntimeException exception) {
        if (exception instanceof DataAccessException) {
            return "lỗi dữ liệu khi lưu, hãy thử lại";
        }
        String message = exception instanceof ResponseStatusException status
                ? status.getReason()
                : exception.getMessage();
        return message == null || message.isBlank() ? "lỗi không xác định" : message;
    }

    private static RecipeResponse preview(ParsedRecipe parsed, UUID userId, UserSummaryResponse user) {
        Recipe recipe = Recipe.builder()
                .recipeCategory(parsed.category())
                .title(parsed.title())
                .description(parsed.description())
                .difficulty(parsed.difficulty())
                .servings(parsed.servings())
                .recipeStatus(RecipeStatus.DRAFT)
                .prepTimeMinutes(parsed.prepTimeMinutes())
                .cookTimeMinutes(parsed.cookTimeMinutes())
                .build();

        Map<UUID, RecipeIngredient> ingredients = new LinkedHashMap<>();
        for (ParsedRecipeIngredient item : parsed.ingredients()) {
            ingredients.put(item.ingredient().getId(), RecipeIngredient.builder()
                    .recipe(recipe)
                    .ingredient(item.ingredient())
                    .rawName(item.ingredient().getName())
                    .quantity(item.quantity())
                    .unit(item.unit())
                    .build());
        }
        Map<UUID, RecipeTool> tools = new LinkedHashMap<>();
        for (ParsedRecipeTool item : parsed.tools()) {
            tools.put(item.tool().getId(), RecipeTool.builder()
                    .recipe(recipe)
                    .tool(item.tool())
                    .rawName(item.tool().getName())
                    .quantity(item.quantity())
                    .build());
        }
        List<RecipeStep> steps = new ArrayList<>();
        for (ParsedStep item : parsed.steps()) {
            RecipeStep step = RecipeStep.builder()
                    .recipe(recipe)
                    .stepNumber(item.stepNumber())
                    .instruction(item.instruction())
                    .durationMinutes(item.durationMinutes())
                    .notes(item.notes())
                    .recipeStepType(item.recipeStepType())
                    .build();
            step.setIngredients(item.ingredients().stream().map(link -> RecipeStepIngredient.builder()
                    .recipeStep(step)
                    .recipeIngredient(ingredients.get(link.ingredient().getId()))
                    .quantity(link.quantity())
                    .note(link.note())
                    .build()).toList());
            step.setTools(item.tools().stream().map(link -> RecipeStepTool.builder()
                    .recipeStep(step)
                    .recipeTool(tools.get(link.tool().getId()))
                    .quantity(link.quantity())
                    .note(link.note())
                    .build()).toList());
            steps.add(step);
        }
        recipe.setIngredients(new ArrayList<>(ingredients.values()));
        recipe.setTools(new ArrayList<>(tools.values()));
        recipe.setSteps(steps);

        RecipeResponse response = RecipeMapper.toDTO(recipe);
        response.setUserId(userId);
        response.setUser(user);
        return response;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw single("File rỗng hoặc chưa được chọn");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw single("Chỉ hỗ trợ file Excel định dạng .xlsx");
        }
        if (file.getSize() > RecipeSheetLayout.MAX_FILE_SIZE_BYTES) {
            throw single("Dung lượng file tối đa 5MB");
        }
    }

    private Workbook openWorkbook(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return new XSSFWorkbook(in);
        } catch (IOException | RuntimeException exception) {
            throw invalidFormat();
        }
    }

    private void validateTitles(List<ParsedRecipe> recipes, UUID userId, List<ImportError> errors) {
        Set<String> existing = recipeRepository.findTitlesByUserId(userId).stream()
                .map(RecipeExcelImportService::titleKey)
                .collect(Collectors.toSet());
        Map<String, ParsedRecipe> seen = new HashMap<>();
        for (ParsedRecipe recipe : recipes) {
            String key = titleKey(recipe.title());
            ParsedRecipe previous = seen.putIfAbsent(key, recipe);
            if (previous != null) {
                errors.add(new ImportError(recipe.sheetName(), recipe.titleRowNumber(), "B",
                        "Tên công thức '" + recipe.title() + "' bị trùng với sheet '" + previous.sheetName() + "'"));
            }
            if (existing.contains(key)) {
                errors.add(new ImportError(recipe.sheetName(), recipe.titleRowNumber(), "B",
                        "Bạn đã có công thức tên '" + recipe.title() + "'"));
            }
        }
    }

    private static RecipeRequest toRequest(ParsedRecipe recipe) {
        return RecipeRequest.builder()
                .categoryId(recipe.category().getId())
                .title(recipe.title())
                .description(recipe.description())
                .difficulty(recipe.difficulty())
                .servings(recipe.servings())
                .recipeStatus(RecipeStatus.DRAFT)
                .prepTimeMinutes(recipe.prepTimeMinutes())
                .cookTimeMinutes(recipe.cookTimeMinutes())
                .ingredients(recipe.ingredients().stream().map(item -> IngredientEmbeddedRequest.builder()
                        .ingredientId(item.ingredient().getId())
                        .quantity(item.quantity())
                        .unit(item.unit())
                        .build()).toList())
                .tools(recipe.tools().stream().map(item -> ToolEmbeddedRequest.builder()
                        .toolId(item.tool().getId())
                        .quantity(item.quantity())
                        .build()).toList())
                .steps(recipe.steps().stream().map(RecipeExcelImportService::toStepRequest).toList())
                .build();
    }

    private static RecipeStepEmbeddedRequest toStepRequest(ParsedStep step) {
        return RecipeStepEmbeddedRequest.builder()
                .stepNumber(step.stepNumber())
                .instruction(step.instruction())
                .durationMinutes(step.durationMinutes())
                .notes(step.notes())
                .recipeStepType(step.recipeStepType())
                .ingredients(step.ingredients().stream().map(item -> IngredientEmbeddedRequest.builder()
                        .ingredientId(item.ingredient().getId())
                        .quantity(item.quantity())
                        .unit(item.unit())
                        .note(item.note())
                        .build()).toList())
                .tools(step.tools().stream().map(item -> ToolEmbeddedRequest.builder()
                        .toolId(item.tool().getId())
                        .quantity(item.quantity())
                        .note(item.note())
                        .build()).toList())
                .build();
    }

    private static String titleKey(String title) {
        return title == null ? "" : title.strip().toLowerCase(Locale.ROOT);
    }

    private static ExcelImportValidationException invalidFormat() {
        return single("File không đúng định dạng Excel (.xlsx) hoặc đã bị hỏng");
    }

    private static ExcelImportValidationException single(String message) {
        return new ExcelImportValidationException(List.of(ImportError.ofFile(message)));
    }
}
