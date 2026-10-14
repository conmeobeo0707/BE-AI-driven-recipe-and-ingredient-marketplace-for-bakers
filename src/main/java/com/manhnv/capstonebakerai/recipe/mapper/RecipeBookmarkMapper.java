package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeBookmarkRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeBookmarkResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeBookmark;

public class RecipeBookmarkMapper {

    public static RecipeBookmarkResponse toDTO(RecipeBookmark entity) {
        if (entity == null) return null;
        return RecipeBookmarkResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe() != null ? entity.getRecipe().getId() : null)
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static RecipeBookmark toEntity(RecipeBookmarkRequest request) {
        if (request == null) return null;
        return new RecipeBookmark();
    }
}