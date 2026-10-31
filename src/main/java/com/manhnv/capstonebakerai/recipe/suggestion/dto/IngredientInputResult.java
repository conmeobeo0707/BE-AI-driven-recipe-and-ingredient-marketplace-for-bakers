package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import java.util.UUID;
import java.util.List;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientCatalogCandidate;

public record IngredientInputResult(
        String input,
        boolean matched,
        UUID catalogIngredientId,
        String canonicalName,
        List<IngredientCatalogCandidate> candidates
) {
}
