package com.manhnv.capstonebakerai.recipe.event;

import java.util.UUID;

public record RatingCreatedEvent(
        UUID ratingId,
        UUID raterUserId,
        UUID recipeId,
        String recipeTitle,
        UUID recipeOwnerId,
        int score
) {
}
