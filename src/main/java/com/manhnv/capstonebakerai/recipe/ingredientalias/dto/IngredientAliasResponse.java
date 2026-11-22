package com.manhnv.capstonebakerai.recipe.ingredientalias.dto;

import java.time.Instant;
import java.util.UUID;

public record IngredientAliasResponse(
        UUID id,
        UUID ingredientId,
        String alias,
        String normalizedAlias,
        String languageCode,
        Instant createdAt
) {
}
