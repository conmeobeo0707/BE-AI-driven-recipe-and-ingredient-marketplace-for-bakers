package com.manhnv.capstonebakerai.recipe.excel.dto;

import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;

import java.util.List;

public record ParsedRecipe(
        String sheetName,
        int titleRowNumber,
        String title,
        String description,
        RecipeCategory category,
        DifficultyEnum difficulty,
        Integer servings,
        List<ParsedStep> steps,
        List<ParsedRecipeIngredient> ingredients,
        List<ParsedRecipeTool> tools,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes
) {
}
