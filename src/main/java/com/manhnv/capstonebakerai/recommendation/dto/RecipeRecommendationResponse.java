package com.manhnv.capstonebakerai.recommendation.dto;

import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;

import java.util.List;

public record RecipeRecommendationResponse(
        String source,
        String recommId,
        List<RecipeResponse> recipes
) {
}
