package com.manhnv.capstonebakerai.recipe.excel.dto;

import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;

import java.util.List;

public record ParsedStep(
        int rowNumber,
        int stepNumber,
        String instruction,
        Integer durationMinutes,
        RecipeStepType recipeStepType,
        String notes,
        List<ParsedStepIngredient> ingredients,
        List<ParsedStepTool> tools
) {
}
