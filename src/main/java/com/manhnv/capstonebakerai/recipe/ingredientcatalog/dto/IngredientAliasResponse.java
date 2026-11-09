package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import java.time.Instant;
import java.util.UUID;

public record IngredientAliasResponse(
        UUID id,
        UUID catalogIngredientId,
        String alias,
        String normalizedAlias,
        String languageCode,
        Instant createdAt
) {
}
