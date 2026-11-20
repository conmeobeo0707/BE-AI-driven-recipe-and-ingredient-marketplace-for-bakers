package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;

public class RecipeStepMapper {

    public static RecipeStepResponse toDTO(RecipeStep entity) {
        if (entity == null) return null;
        return RecipeStepResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe() != null ? entity.getRecipe().getId() : null)
                .stepNumber(entity.getStepNumber())
                .instruction(entity.getInstruction())
                .durationMinutes(entity.getDurationMinutes())
                .notes(entity.getNotes())
                .recipeStepType(entity.getRecipeStepType())
                .imageIds(entity.getImages() != null
                        ? entity.getImages().stream().map(image -> image.getMedia().getId()).toList()
                        : java.util.Collections.emptyList())
                .imageUrls(entity.getImages() != null
                        ? entity.getImages().stream().map(image -> image.getMedia().getUrl()).toList()
                        : java.util.Collections.emptyList())
                .ingredients(entity.getIngredients() == null ? java.util.List.of()
                        : entity.getIngredients().stream().map(RecipeStepIngredientMapper::toDTO).toList())
                .tools(entity.getTools() == null ? java.util.List.of()
                        : entity.getTools().stream().map(RecipeStepToolMapper::toDTO).toList())
                .build();
    }

    public static RecipeStep toEntity(RecipeStepRequest request) {
        if (request == null) return null;
        RecipeStep entity = new RecipeStep();
        entity.setStepNumber(request.getStepNumber());
        entity.setInstruction(request.getInstruction());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setNotes(request.getNotes());
        entity.setRecipeStepType(request.getRecipeStepType());
        return entity;
    }
}
