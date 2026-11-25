package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.entity.Tool;

public class RecipeStepToolMapper {

    public static RecipeStepToolResponse toDTO(RecipeStepTool entity) {
        if (entity == null) return null;
        RecipeTool link = entity.getRecipeTool();
        Tool tool = link.getTool();
        return RecipeStepToolResponse.builder()
                .id(entity.getId())
                .recipeStepId(entity.getRecipeStep().getId())
                .recipeToolId(link.getId())
                .toolId(tool == null ? null : tool.getId())
                .toolName(tool == null ? link.getRawName() : tool.getName())
                .rawName(link.getRawName())
                .quantity(entity.getQuantity())
                .build();
    }

    public static RecipeStepTool toEntity(RecipeStepToolRequest request) {
        if (request == null) return null;
        RecipeStepTool entity = new RecipeStepTool();
        entity.setQuantity(request.getQuantity());
        return entity;
    }
}
