package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeIngredientResponse;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;

public class RecipeIngredientMapper {

    public static RecipeIngredientResponse toDTO(RecipeIngredient entity) {
        if (entity == null) return null;
        Ingredient ingredient = entity.getIngredient();
        return RecipeIngredientResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe().getId())
                .ingredientId(ingredient == null ? null : ingredient.getId())
                .ingredientName(ingredient == null ? entity.getRawName() : ingredient.getName())
                .rawName(entity.getRawName())
                .note(entity.getNote())
                .quantity(entity.getQuantity())
                .unit(entity.getUnit())
                .build();
    }

    public static RecipeIngredient toEntity(RecipeIngredientRequest request) {
        if (request == null) return null;
        RecipeIngredient entity = new RecipeIngredient();
        entity.setQuantity(request.getQuantity());
        entity.setUnit(request.getUnit());
        entity.setNote(request.getNote());
        return entity;
    }
}
