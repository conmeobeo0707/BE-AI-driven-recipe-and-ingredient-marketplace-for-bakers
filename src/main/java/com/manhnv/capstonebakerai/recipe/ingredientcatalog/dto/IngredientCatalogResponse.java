package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import java.time.Instant;
import java.util.UUID;

public record IngredientCatalogResponse(
        UUID id,
        String canonicalName,
        String normalizedName,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
