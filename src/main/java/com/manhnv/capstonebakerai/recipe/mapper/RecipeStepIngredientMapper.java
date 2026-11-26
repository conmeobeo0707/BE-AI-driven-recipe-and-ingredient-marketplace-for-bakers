package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepIngredientItemResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepIngredientResponse;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;

public class RecipeStepIngredientMapper {

    public static RecipeStepIngredientResponse toDTO(RecipeStepIngredient entity) {
        if (entity == null) return null;
        RecipeIngredient link = entity.getRecipeIngredient();
        Ingredient ingredient = link.getIngredient();
        return RecipeStepIngredientResponse.builder()
                .id(entity.getId())
                .recipeStepId(entity.getRecipeStep().getId())
                .recipeIngredientId(link.getId())
                .ingredientId(ingredient == null ? null : ingredient.getId())
                .ingredientName(ingredient == null ? link.getRawName() : ingredient.getName())
                .rawName(link.getRawName())
                .quantity(entity.getQuantity())
                .unit(link.getUnit())
                .note(entity.getNote())
                .build();
    }

    public static RecipeStepIngredientItemResponse toItem(RecipeStepIngredient entity) {
        if (entity == null) return null;
        RecipeIngredient link = entity.getRecipeIngredient();
        Ingredient ingredient = link.getIngredient();
        return RecipeStepIngredientItemResponse.builder()
                .id(entity.getId())
                .recipeIngredientId(link.getId())
                .ingredientId(ingredient == null ? null : ingredient.getId())
                .ingredientName(ingredient == null ? link.getRawName() : ingredient.getName())
                .rawName(link.getRawName())
                .quantity(entity.getQuantity())
                .unit(link.getUnit())
                .note(entity.getNote())
                .build();
    }

    public static RecipeStepIngredient toEntity(RecipeStepIngredientRequest request) {
        if (request == null) return null;
        RecipeStepIngredient entity = new RecipeStepIngredient();
        entity.setQuantity(request.getQuantity());
        entity.setNote(trimToNull(request.getNote()));
        return entity;
    }

    public static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
