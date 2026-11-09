package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import java.util.UUID;

public record LinkByNameResponse(
        UUID catalogIngredientId,
        String canonicalName,
        String name,
        boolean aliasCreated,
        int linkedIngredientCount
) {
}
