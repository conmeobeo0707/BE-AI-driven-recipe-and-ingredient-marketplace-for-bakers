package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;

import java.math.BigDecimal;
import java.util.List;

public record ExtractedRecipe(
        boolean isRecipe,
        String categoryName,
        String title,
        String description,
        DifficultyEnum difficulty,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        Integer servings,
        List<Tool> tools,
        List<Ingredient> ingredients,
        List<Step> steps
) {

    public static ExtractedRecipe notRecipe() {
        return new ExtractedRecipe(false, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of());
    }

    public record Tool(String key, String name, BigDecimal quantity, String note) {
    }

    public record Ingredient(String key, String name, String unit, BigDecimal quantity, String note) {
    }

    public record Step(
            Integer stepNumber,
            String instruction,
            Integer durationMinutes,
            String notes,
            RecipeStepType recipeStepType,
            List<ItemRef> tools,
            List<ItemRef> ingredients
    ) {
    }

    public record ItemRef(String key, BigDecimal quantity) {
    }
}
