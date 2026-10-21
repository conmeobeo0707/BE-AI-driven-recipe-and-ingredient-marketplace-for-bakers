package com.manhnv.capstonebakerai.recipe.event;

import java.util.UUID;

public record CommentCreatedEvent(
        UUID commentId,
        UUID commenterId,
        UUID recipeId,
        String recipeTitle,
        UUID recipeOwnerId,
        UUID parentCommentOwnerId
) {
}
