package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import java.util.List;

public record UnlinkedIngredientGroup(
        String name,
        String normalizedName,
        long rowCount,
        List<String> spellings,
        List<IngredientCatalogCandidate> candidates
) {
}
