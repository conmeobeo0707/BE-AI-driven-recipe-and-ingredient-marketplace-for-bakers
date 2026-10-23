package com.manhnv.capstonebakerai.recommendation.event;

import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;

import java.time.Instant;
import java.util.UUID;

public record RecipeInteractionRecordedEvent(
        UUID interactionId,
        UUID customerId,
        UUID recipeId,
        RecipeInteractionType type,
        Integer ratingScore,
        String recommId,
        Instant occurredAt
) {
}
