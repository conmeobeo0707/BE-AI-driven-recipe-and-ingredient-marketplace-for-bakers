package com.manhnv.capstonebakerai.recipe.exception;

import com.manhnv.capstonebakerai.common.exception.AppException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class RecipeStepErrors {

    private RecipeStepErrors() {}

    public static AppException ingredientDuplicateInStep(UUID recipeIngredientId, Integer stepNumber) {
        return error(RecipeErrorCode.STEP_INGREDIENT_DUPLICATE,
                RecipeErrorCode.STEP_INGREDIENT_DUPLICATE.getMessage(),
                "recipeIngredientId", recipeIngredientId, "stepNumber", stepNumber);
    }

    public static AppException toolDuplicateInStep(UUID recipeToolId, Integer stepNumber) {
        return error(RecipeErrorCode.STEP_TOOL_DUPLICATE,
                RecipeErrorCode.STEP_TOOL_DUPLICATE.getMessage(),
                "recipeToolId", recipeToolId, "stepNumber", stepNumber);
    }

    public static AppException duplicateIds(String itemType, String scope) {
        return error(RecipeErrorCode.DUPLICATE_ITEM_IDS,
                "The same " + itemType + " appears more than once in the " + scope,
                "itemType", itemType, "scope", scope);
    }

    public static AppException ingredientTotalExceeds(String idField, UUID id, Integer stepNumber,
                                                      BigDecimal total, BigDecimal recipeQuantity) {
        return error(RecipeErrorCode.STEP_QUANTITY_EXCEEDS_RECIPE,
                "Total quantity across steps (" + total.toPlainString() + ") exceeds the recipe quantity ("
                        + recipeQuantity.toPlainString() + ")",
                idField, id, "stepNumber", stepNumber, "total", total, "recipeQuantity", recipeQuantity);
    }

    public static AppException toolQuantityExceeds(String idField, UUID id, Integer stepNumber,
                                                   BigDecimal quantity, BigDecimal recipeQuantity) {
        return error(RecipeErrorCode.STEP_QUANTITY_EXCEEDS_RECIPE,
                "Tool quantity in a step (" + quantity.toPlainString() + ") exceeds the recipe quantity ("
                        + recipeQuantity.toPlainString() + ")",
                idField, id, "stepNumber", stepNumber, "quantity", quantity, "recipeQuantity", recipeQuantity);
    }

    public static AppException ingredientTotalMismatch(UUID ingredientId, BigDecimal total, BigDecimal recipeQuantity) {
        return error(RecipeErrorCode.STEP_QUANTITY_MISMATCH,
                "Total quantity across steps (" + total.toPlainString() + ") must equal the recipe quantity ("
                        + recipeQuantity.toPlainString() + ")",
                "ingredientId", ingredientId, "total", total, "recipeQuantity", recipeQuantity);
    }

    public static AppException notInRecipe(String idField, UUID id, Integer stepNumber) {
        return error(RecipeErrorCode.STEP_ITEM_NOT_IN_RECIPE,
                RecipeErrorCode.STEP_ITEM_NOT_IN_RECIPE.getMessage(),
                idField, id, "stepNumber", stepNumber);
    }

    public static AppException usedBySteps(String idField, UUID id) {
        return error(RecipeErrorCode.RECIPE_ITEM_USED_BY_STEPS,
                RecipeErrorCode.RECIPE_ITEM_USED_BY_STEPS.getMessage(),
                idField, id);
    }

    public static AppException quantityBelowStepUsage(String idField, UUID id, BigDecimal recipeQuantity, BigDecimal used) {
        return error(RecipeErrorCode.RECIPE_QUANTITY_BELOW_STEP_USAGE,
                "Recipe quantity (" + recipeQuantity.toPlainString() + ") is less than the quantity used by steps ("
                        + used.toPlainString() + ")",
                idField, id, "recipeQuantity", recipeQuantity, "used", used);
    }

    private static AppException error(RecipeErrorCode code, String message, Object... keyValues) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            data.put((String) keyValues[i], keyValues[i + 1]);
        }
        return new AppException(code, message, data);
    }
}
