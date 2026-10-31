package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import java.util.List;
import java.util.UUID;

public record IngredientBackfillPreviewResponse(long totalUnmapped, List<Item> items) {
    public record Item(
            UUID ingredientId,
            UUID recipeId,
            String ingredientName,
            UUID proposedCatalogIngredientId,
            String proposedCanonicalName,
            List<IngredientCatalogCandidate> candidates
    ) {
    }
}
