package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import java.util.List;
import java.util.UUID;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientCatalogCandidate;

public record IngredientImageRecognitionResponse(
        String inputMode,
        boolean requiresConfirmation,
        List<Item> ingredients
) {

    public record Item(
            String observedName,
            boolean uncertain,
            boolean matched,
            UUID catalogIngredientId,
            String canonicalName,
            List<IngredientCatalogCandidate> candidates
    ) {
    }
}
