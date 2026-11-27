package com.manhnv.capstonebakerai.recipe.ingredientalias.dto;

import java.util.UUID;

public record IngredientCandidate(
        UUID ingredientId,
        String ingredientName,
        String matchedName,
        double similarity,
        MatchType matchType
) {
    public enum MatchType {
        EXACT, PARTIAL, FUZZY
    }
}
