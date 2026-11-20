package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;

public class RecipeStepToolMapper {

    public static RecipeStepToolResponse toDTO(RecipeStepTool entity) {
        if (entity == null) return null;
        return RecipeStepToolResponse.builder()
                .id(entity.getId())
                .recipeStepId(entity.getRecipeStep().getId())
                .toolId(entity.getTool().getId())
                .toolName(entity.getTool().getName())
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
