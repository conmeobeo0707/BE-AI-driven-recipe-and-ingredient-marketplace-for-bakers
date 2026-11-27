package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.ToolEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.recipe.exception.RecipeErrorCode;
import com.manhnv.capstonebakerai.recipe.repository.RecipeIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeToolRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipeStepResourceHelperEmbeddedTest {

    private final RecipeStepIngredientRepository stepIngredientRepository = mock(RecipeStepIngredientRepository.class);
    private final RecipeStepToolRepository stepToolRepository = mock(RecipeStepToolRepository.class);
    private final RecipeStepResourceHelper helper = new RecipeStepResourceHelper(stepIngredientRepository,
            stepToolRepository, mock(RecipeIngredientRepository.class), mock(RecipeToolRepository.class));

    private static final UUID OREO = UUID.randomUUID();
    private static final UUID SUGAR = UUID.randomUUID();
    private static final UUID WHISK = UUID.randomUUID();

    private static IngredientEmbeddedRequest ingredient(String ref, UUID id, String rawName, int qty) {
        return IngredientEmbeddedRequest.builder().ref(ref).ingredientId(id).rawName(rawName)
                .quantity(BigDecimal.valueOf(qty)).unit("g").build();
    }

    private static ToolEmbeddedRequest tool(String ref, UUID id, String rawName, int qty) {
        return ToolEmbeddedRequest.builder().ref(ref).toolId(id).rawName(rawName).quantity(BigDecimal.valueOf(qty)).build();
    }

    private static IngredientEmbeddedRequest stepIngredient(String ref, UUID id, int qty) {
        return IngredientEmbeddedRequest.builder().ref(ref).ingredientId(id).quantity(BigDecimal.valueOf(qty)).build();
    }

    private static ToolEmbeddedRequest stepTool(String ref, UUID id, int qty) {
        return ToolEmbeddedRequest.builder().ref(ref).toolId(id).quantity(BigDecimal.valueOf(qty)).build();
    }

    private static RecipeStepEmbeddedRequest step(int number, List<IngredientEmbeddedRequest> ingredients,
                                                  List<ToolEmbeddedRequest> tools) {
        return RecipeStepEmbeddedRequest.builder().stepNumber(number).instruction("x")
                .recipeStepType(RecipeStepType.PREPARATION).ingredients(ingredients).tools(tools).build();
    }

    private static void assertCode(Throwable thrown, RecipeErrorCode code) {
        assertThat(thrown).isInstanceOf(AppException.class);
        assertThat(((AppException) thrown).getErrorCode()).isEqualTo(code);
    }

    private void validateAll(RecipeStatus status, List<IngredientEmbeddedRequest> ingredients,
                             List<ToolEmbeddedRequest> tools, List<RecipeStepEmbeddedRequest> steps) {
        helper.validateCatalogItems(status, ingredients, tools);
        helper.validateEmbeddedRecipe(ingredients, tools, steps);
    }

    @Test
    void legacyRequestWithoutRefsStillValidates() {
        var ingredients = List.of(ingredient(null, OREO, null, 100), ingredient(null, SUGAR, null, 20));
        var tools = List.of(tool(null, WHISK, null, 1));
        var steps = List.of(
                step(1, List.of(stepIngredient(null, OREO, 60), stepIngredient(null, SUGAR, 20)), List.of(stepTool(null, WHISK, 1))),
                step(2, List.of(stepIngredient(null, OREO, 40)), List.of()));

        assertThatCode(() -> validateAll(RecipeStatus.PUBLISHED, ingredients, tools, steps)).doesNotThrowAnyException();
    }

    @Test
    void legacyRequestStillRejectsStepIngredientMissingFromRecipe() {
        var ingredients = List.of(ingredient(null, OREO, null, 100));
        var steps = List.of(step(1, List.of(stepIngredient(null, SUGAR, 100)), List.of()));

        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PUBLISHED, ingredients, List.of(), steps)),
                RecipeErrorCode.STEP_ITEM_NOT_IN_RECIPE);
    }

    @Test
    void personalRecipeAcceptsUnmatchedItemsReferencedByRef() {
        var ingredients = List.of(ingredient("a1", OREO, null, 1), ingredient("a2", null, "Sữa tươi không đường", 50));
        var tools = List.of(tool("t1", null, "Cây lăn bột", 1));
        var steps = List.of(
                step(1, List.of(stepIngredient("a1", null, 1), stepIngredient("a2", null, 50)), List.of(stepTool("t1", null, 1))),
                step(2, List.of(), List.of()));

        assertThatCode(() -> validateAll(RecipeStatus.PERSONAL, ingredients, tools, steps)).doesNotThrowAnyException();
    }

    @Test
    void twoUnmatchedRowsWithDifferentRefsAreNotDuplicates() {
        var ingredients = List.of(ingredient("a1", null, "Bột nở", 5), ingredient("a2", null, "Vani", 2));
        var steps = List.of(step(1, List.of(stepIngredient("a1", null, 5), stepIngredient("a2", null, 2)), List.of()));

        assertThatCode(() -> validateAll(RecipeStatus.PERSONAL, ingredients, List.of(), steps)).doesNotThrowAnyException();
    }

    @Test
    void stepMayReferenceRowByCatalogIdEvenWhenRecipeRowHasRef() {
        var ingredients = List.of(ingredient("a1", OREO, null, 10), ingredient("a2", null, "Bột nở", 5));
        var steps = List.of(step(1, List.of(stepIngredient(null, OREO, 10), stepIngredient("a2", null, 5)), List.of()));

        assertThatCode(() -> validateAll(RecipeStatus.PERSONAL, ingredients, List.of(), steps)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = RecipeStatus.class, names = "PERSONAL", mode = EnumSource.Mode.EXCLUDE)
    void unmatchedIngredientIsRejectedOutsidePersonal(RecipeStatus status) {
        var ingredients = List.of(ingredient("a1", null, "Bột nở", 5));

        assertCode(catchThrowable(() -> helper.validateCatalogItems(status, ingredients, List.of())),
                RecipeErrorCode.UNMATCHED_ITEM_NOT_ALLOWED);
    }

    @Test
    void unmatchedToolIsRejectedOutsidePersonal() {
        assertCode(catchThrowable(() -> helper.validateCatalogItems(RecipeStatus.PUBLISHED, List.of(),
                List.of(tool("t1", null, "Cây lăn bột", 1)))), RecipeErrorCode.UNMATCHED_ITEM_NOT_ALLOWED);
    }

    @Test
    void unmatchedRowRequiresRawName() {
        assertCode(catchThrowable(() -> helper.validateCatalogItems(RecipeStatus.PERSONAL,
                List.of(ingredient("a1", null, "  ", 5)), List.of())), RecipeErrorCode.RAW_NAME_REQUIRED);
        assertCode(catchThrowable(() -> helper.validateCatalogItems(RecipeStatus.PERSONAL, List.of(),
                List.of(tool("t1", null, null, 1)))), RecipeErrorCode.RAW_NAME_REQUIRED);
    }

    @Test
    void unmatchedRowRequiresRef() {
        assertCode(catchThrowable(() -> helper.validateCatalogItems(RecipeStatus.PERSONAL,
                List.of(ingredient(null, null, "Bột nở", 5)), List.of())), RecipeErrorCode.REF_REQUIRED);
        assertCode(catchThrowable(() -> helper.validateCatalogItems(RecipeStatus.PERSONAL, List.of(),
                List.of(tool(" ", null, "Cây lăn bột", 1)))), RecipeErrorCode.REF_REQUIRED);
    }

    @Test
    void duplicateRefsAreRejected() {
        var ingredients = List.of(ingredient("a1", null, "Bột nở", 5), ingredient("a1", null, "Vani", 2));
        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, ingredients, List.of(), List.of())),
                RecipeErrorCode.DUPLICATE_ITEM_IDS);

        var tools = List.of(tool("t1", null, "Cây lăn bột", 1), tool("t1", null, "Khuôn", 1));
        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, List.of(), tools, List.of())),
                RecipeErrorCode.DUPLICATE_ITEM_IDS);
    }

    @Test
    void sameCatalogIdUnderDifferentRefsIsStillRejected() {
        var ingredients = List.of(ingredient("a1", SUGAR, null, 5), ingredient("a2", SUGAR, null, 2));

        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, ingredients, List.of(), List.of())),
                RecipeErrorCode.DUPLICATE_ITEM_IDS);
    }

    @Test
    void stepItemWithUnknownRefOrNoKeyIsRejected() {
        var ingredients = List.of(ingredient("a1", null, "Bột nở", 5));
        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, ingredients, List.of(),
                List.of(step(1, List.of(stepIngredient("zzz", null, 5)), List.of())))), RecipeErrorCode.STEP_ITEM_NOT_IN_RECIPE);
        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, ingredients, List.of(),
                List.of(step(1, List.of(stepIngredient(null, null, 5)), List.of())))), RecipeErrorCode.STEP_ITEM_NOT_IN_RECIPE);

        var tools = List.of(tool("t1", null, "Cây lăn bột", 1));
        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, List.of(), tools,
                List.of(step(1, List.of(), List.of(stepTool("t2", null, 1)))))), RecipeErrorCode.STEP_ITEM_NOT_IN_RECIPE);
    }

    @Test
    void quantityRulesApplyToUnmatchedRows() {
        var ingredients = List.of(ingredient("a2", null, "Sữa tươi", 50));
        Throwable mismatch = catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, ingredients, List.of(),
                List.of(step(1, List.of(stepIngredient("a2", null, 40)), List.of()))));
        assertCode(mismatch, RecipeErrorCode.STEP_QUANTITY_MISMATCH);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((AppException) mismatch).getData();
        assertThat(data).containsEntry("ref", "a2");

        var tools = List.of(tool("t1", null, "Cây lăn bột", 1));
        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, List.of(), tools,
                List.of(step(1, List.of(), List.of(stepTool("t1", null, 2)))))), RecipeErrorCode.STEP_QUANTITY_EXCEEDS_RECIPE);
    }

    @Test
    void sameRowTwiceInOneStepIsRejected() {
        var ingredients = List.of(ingredient("a1", OREO, null, 10));
        var steps = List.of(step(1, List.of(stepIngredient("a1", null, 5), stepIngredient(null, OREO, 5)), List.of()));

        assertCode(catchThrowable(() -> validateAll(RecipeStatus.PERSONAL, ingredients, List.of(), steps)),
                RecipeErrorCode.DUPLICATE_ITEM_IDS);
    }

    @Test
    void replaceEmbeddedResourcesLinksStepItemsToRowsByRef() {
        when(stepIngredientRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(stepToolRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        RecipeIngredient oreoRow = RecipeIngredient.builder()
                .ingredient(Ingredient.builder().id(OREO).name("Oreo").build()).rawName("Oreo").build();
        RecipeIngredient milkRow = RecipeIngredient.builder().rawName("Sữa tươi").build();
        RecipeIngredient bakingPowderRow = RecipeIngredient.builder().rawName("Bột nở").build();
        RecipeTool rollerRow = RecipeTool.builder().rawName("Cây lăn bột").build();
        Map<String, RecipeIngredient> ingredientRows = new LinkedHashMap<>();
        ingredientRows.put("a1", oreoRow);
        ingredientRows.put("a2", milkRow);
        ingredientRows.put("a3", bakingPowderRow);
        RecipeStep step = RecipeStep.builder().id(UUID.randomUUID()).stepNumber(1).build();

        helper.replaceEmbeddedResources(step,
                List.of(stepIngredient("a3", null, 5), stepIngredient(null, OREO, 1), stepIngredient("a2", null, 50)),
                List.of(stepTool("t1", null, 1)),
                ingredientRows, Map.of("t1", rollerRow));

        assertThat(step.getIngredients()).extracting(link -> link.getRecipeIngredient())
                .containsExactly(bakingPowderRow, oreoRow, milkRow);
        assertThat(step.getTools()).extracting(link -> link.getRecipeTool()).containsExactly(rollerRow);
    }

    private static Throwable catchThrowable(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        return org.assertj.core.api.Assertions.catchThrowable(callable);
    }
}
