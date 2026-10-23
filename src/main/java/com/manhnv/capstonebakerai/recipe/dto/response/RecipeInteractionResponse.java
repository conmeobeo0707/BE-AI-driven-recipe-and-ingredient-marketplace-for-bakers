package com.manhnv.capstonebakerai.recipe.dto.response;

import com.manhnv.capstonebakerai.recipe.enums.InteractionSource;
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
    private String searchQuery;
    private InteractionSource source;
    private Instant occurredAt;
}
