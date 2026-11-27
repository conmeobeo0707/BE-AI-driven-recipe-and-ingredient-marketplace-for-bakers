package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import java.util.List;
import java.util.UUID;
import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.IngredientCandidate;

public record IngredientImageRecognitionResponse(
        int remainingDailyQuota,
        int pointsCharged,
        int pointsBalance,
        List<Item> ingredients
) {

    public record Item(
            String observedName,
            String vietnameseName,
            boolean uncertain,
            boolean matched,
            UUID ingredientId,
            String ingredientName,
            List<IngredientCandidate> candidates
    ) {
    }
}
