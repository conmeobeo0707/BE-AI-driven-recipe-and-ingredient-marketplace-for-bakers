package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepIngredientResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;

public class RecipeStepIngredientMapper {

    public static RecipeStepIngredientResponse toDTO(RecipeStepIngredient entity) {
        if (entity == null) return null;
        return RecipeStepIngredientResponse.builder()
                .id(entity.getId())
                .recipeStepId(entity.getRecipeStep().getId())
                .ingredientId(entity.getIngredient().getId())
                .ingredientName(entity.getIngredient().getName())
                .quantity(entity.getQuantity())
                .unit(entity.getIngredient().getUnit())
                .build();
    }

    public static RecipeStepIngredient toEntity(RecipeStepIngredientRequest request) {
        if (request == null) return null;
        RecipeStepIngredient entity = new RecipeStepIngredient();
        entity.setQuantity(request.getQuantity());
        return entity;
    }
}
