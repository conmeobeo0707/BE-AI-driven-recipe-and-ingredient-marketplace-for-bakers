package com.manhnv.capstonebakerai.recipe.suggestion.service;

import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientSuggestionResponse;

import java.util.List;
import java.util.UUID;

public interface RecipeSuggestionService {

    IngredientSuggestionResponse suggest(
            List<String> ingredientNames,
            Integer requestedLimit,
            String inputMode,
            boolean allowPartial
    );

    IngredientSuggestionResponse suggestByIds(
            List<UUID> catalogIngredientIds,
            List<String> unmatchedIngredients,
            Integer requestedLimit,
            String inputMode,
            boolean allowPartial
    );
}
