package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import java.util.List;
import java.util.UUID;

public record RecipeSuggestionItem(
        UUID recipeId,
        String title,
        int matchedIngredientCount,
        int totalIngredientCount,
        double coverage,
        List<String> matchedIngredients,
        List<String> missingIngredients
) {
}
