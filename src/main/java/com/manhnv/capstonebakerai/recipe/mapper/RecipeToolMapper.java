package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.entity.Tool;

public class RecipeToolMapper {

    public static RecipeToolResponse toDTO(RecipeTool entity) {
        if (entity == null) return null;
        Tool tool = entity.getTool();
        return RecipeToolResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe().getId())
                .toolId(tool == null ? null : tool.getId())
                .toolName(tool == null ? entity.getRawName() : tool.getName())
                .rawName(entity.getRawName())
                .note(entity.getNote())
                .quantity(entity.getQuantity())
                .build();
    }

    public static RecipeTool toEntity(RecipeToolRequest request) {
        if (request == null) return null;
        RecipeTool entity = new RecipeTool();
        entity.setQuantity(request.getQuantity());
        entity.setNote(request.getNote());
        return entity;
    }
}
