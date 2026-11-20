package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;

public class RecipeToolMapper {

    public static RecipeToolResponse toDTO(RecipeTool entity) {
        if (entity == null) return null;
        return RecipeToolResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe().getId())
                .toolId(entity.getTool().getId())
                .toolName(entity.getTool().getName())
                .quantity(entity.getQuantity())
                .build();
    }

    public static RecipeTool toEntity(RecipeToolRequest request) {
        if (request == null) return null;
        RecipeTool entity = new RecipeTool();
        entity.setQuantity(request.getQuantity());
        return entity;
    }
}
