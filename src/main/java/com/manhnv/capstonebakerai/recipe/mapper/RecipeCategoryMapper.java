package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCategoryRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCategoryResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;

public class RecipeCategoryMapper {

    public static RecipeCategoryResponse toDTO(RecipeCategory entity) {
        if (entity == null) return null;
        return RecipeCategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public static RecipeCategory toEntity(RecipeCategoryRequest request) {
        if (request == null) return null;
        return RecipeCategory.builder()
                .name(request.getName())
                .build();
    }
}