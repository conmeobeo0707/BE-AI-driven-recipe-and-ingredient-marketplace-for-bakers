package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import java.util.List;

public record IngredientSuggestionResponse(
        String inputMode,
        List<IngredientInputResult> inputIngredients,
        List<String> unmatchedIngredients,
        List<RecipeSuggestionItem> recipes,
        int totalInputCount,
        int matchedInputCount,
        int resolvedIngredientCount,
        boolean requiresResolution,
        String coverageBasis,
        List<String> warnings
) {
}
