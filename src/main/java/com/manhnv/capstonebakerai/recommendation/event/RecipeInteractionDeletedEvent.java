package com.manhnv.capstonebakerai.recommendation.event;

import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;

import java.util.UUID;

public record RecipeInteractionDeletedEvent(
        UUID customerId,
        UUID recipeId,
        RecipeInteractionType type
) {
}
