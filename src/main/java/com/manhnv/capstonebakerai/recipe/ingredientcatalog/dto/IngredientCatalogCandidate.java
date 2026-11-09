package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import java.util.UUID;

public record IngredientCatalogCandidate(
        UUID catalogIngredientId,
        String canonicalName,
        String matchedName,
        double similarity,
        MatchType matchType
) {
    public enum MatchType {
        EXACT, PARTIAL, FUZZY
    }
}
