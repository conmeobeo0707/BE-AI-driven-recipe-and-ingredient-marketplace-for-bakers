package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.ToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.ToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.Tool;

public class ToolMapper {

    public static ToolResponse toDTO(Tool entity) {
        if (entity == null) return null;
        return ToolResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe() != null ? entity.getRecipe().getId() : null)
                .name(entity.getName())
                .build();
    }

    public static Tool toEntity(ToolRequest request) {
        if (request == null) return null;
        Tool entity = new Tool();
        entity.setName(request.getName());
        return entity;
    }
}