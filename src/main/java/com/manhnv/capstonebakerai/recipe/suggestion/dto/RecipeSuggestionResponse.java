package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import java.util.List;
import java.util.UUID;

public record RecipeSuggestionResponse(
        List<SelectedIngredient> selectedIngredients,
        List<RecipeSuggestionItem> recipes,
        String coverageBasis,
        List<String> warnings
) {

    public record SelectedIngredient(
            UUID catalogIngredientId,
            String canonicalName
    ) {
    }
}
