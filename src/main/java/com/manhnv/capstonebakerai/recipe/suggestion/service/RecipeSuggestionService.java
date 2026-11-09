package com.manhnv.capstonebakerai.recipe.suggestion.service;

import com.manhnv.capstonebakerai.recipe.suggestion.dto.RecipeSuggestionResponse;

import java.util.List;
import java.util.UUID;

public interface RecipeSuggestionService {

    RecipeSuggestionResponse suggest(
            List<UUID> catalogIngredientIds,
            Integer requestedLimit
    );
}
