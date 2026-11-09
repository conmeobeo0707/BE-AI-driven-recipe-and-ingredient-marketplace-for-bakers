package com.manhnv.capstonebakerai.recommendation.dto;

import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;

import java.util.List;

// modelVersion: trained model in use (null = default weights);
// recommId: id of this list, sent back by the FE when the customer views/bookmarks/rates a recommended recipe.
public record RecipeRecommendationResponse(
        Integer modelVersion,
        String recommId,
        List<Item> recipes
) {
    public record Item(RecipeResponse recipe, double score, List<String> reasons) {
    }
}
