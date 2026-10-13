package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.IngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.IngredientResponse;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;

public class IngredientMapper {

    public static IngredientResponse toDTO(Ingredient entity) {
        if (entity == null) return null;
        return IngredientResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe() != null ? entity.getRecipe().getId() : null)
                .name(entity.getName())
                .unit(entity.getUnit())
                .quantity(entity.getQuantity())
                .build();
    }

    public static Ingredient toEntity(IngredientRequest request) {
        if (request == null) return null;
        Ingredient entity = new Ingredient();
        entity.setName(request.getName());
        entity.setUnit(request.getUnit());
        entity.setQuantity(request.getQuantity());
        return entity;
    }
}