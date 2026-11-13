package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeIngredientResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;

public class RecipeIngredientMapper {

    public static RecipeIngredientResponse toDTO(RecipeIngredient entity) {
        if (entity == null) return null;
        return RecipeIngredientResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe().getId())
                .ingredientId(entity.getIngredient().getId())
                .ingredientName(entity.getIngredient().getName())
                .quantity(entity.getQuantity())
                .unit(entity.getIngredient().getUnit())
                .build();
    }

    public static RecipeIngredient toEntity(RecipeIngredientRequest request) {
        if (request == null) return null;
        RecipeIngredient entity = new RecipeIngredient();
        entity.setQuantity(request.getQuantity());
        return entity;
    }
}
