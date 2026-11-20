package com.manhnv.capstonebakerai.recipe.dto.response;

import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeInteractionResponse {
    private UUID id;
    private RecipeInteractionType type;
    private UUID recipeId;
    private String recipeTitle;
    private Integer ratingScore;
    private String recommId;
    private Instant occurredAt;
    // BOOKMARK/RATING only: true while this row is the customer's current bookmark/rating; null for VIEW.
    private Boolean active;
}
