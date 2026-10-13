package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRatingRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeRatingResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeRating;

public class RecipeRatingMapper {

    public static RecipeRatingResponse toDTO(RecipeRating entity) {
        if (entity == null) return null;
        return RecipeRatingResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe() != null ? entity.getRecipe().getId() : null)
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getId() : null)
                .score(entity.getScore())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static RecipeRating toEntity(RecipeRatingRequest request) {
        if (request == null) return null;
        return new RecipeRating();
    }
}