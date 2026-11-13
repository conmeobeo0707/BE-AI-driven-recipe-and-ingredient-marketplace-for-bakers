package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;

import java.util.Collections;
import java.util.stream.Collectors;

public class RecipeMapper {

    public static RecipeResponse toDTO(Recipe entity) {
        if (entity == null) return null;
        return RecipeResponse.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getUserId() : null)
                .categoryId(entity.getRecipeCategory() != null ? entity.getRecipeCategory().getId() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .videoId(entity.getMedia() != null ? entity.getMedia().getId() : null)
                .videoUrl(entity.getMedia() != null ? entity.getMedia().getUrl() : null)
                .imageIds(entity.getImages() != null
                        ? entity.getImages().stream().map(image -> image.getMedia().getId()).toList()
                        : Collections.emptyList())
                .imageUrls(entity.getImages() != null
                        ? entity.getImages().stream().map(image -> image.getMedia().getUrl()).toList()
                        : Collections.emptyList())
                .difficulty(entity.getDifficulty())
                .prepTimeMinutes(entity.getPrepTimeMinutes())
                .cookTimeMinutes(entity.getCookTimeMinutes())
                .servings(entity.getServings())
                .averageRating(entity.getAverageRating())
                .recipeStatus(entity.getRecipeStatus())
                .createdAt(entity.getCreatedAt())
                .publishedAt(entity.getPublishedAt())
                .viewCount(entity.getViewCount())
                .bookmarkCount(entity.getBookmarkCount())
                .ratingCount(entity.getRatingCount())
                .tools(entity.getTools() != null
                        ? entity.getTools().stream().map(link -> com.manhnv.capstonebakerai.recipe.dto.response.ToolResponse.builder().id(link.getTool().getId()).name(link.getTool().getName()).recipeId(entity.getId()).quantity(link.getQuantity()).build()).collect(Collectors.toList())
                        : Collections.emptyList())
                .ingredients(entity.getIngredients() != null
                        ? entity.getIngredients().stream().map(link -> com.manhnv.capstonebakerai.recipe.dto.response.IngredientResponse.builder().id(link.getIngredient().getId()).recipeId(entity.getId()).name(link.getIngredient().getName()).unit(link.getIngredient().getUnit()).quantity(link.getQuantity()).build()).collect(Collectors.toList())
                        : Collections.emptyList())
                .steps(entity.getSteps() != null
                        ? entity.getSteps().stream().map(RecipeStepMapper::toDTO).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    public static Recipe toEntity(RecipeRequest request) {
        if (request == null) return null;
        Recipe entity = new Recipe();
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setDifficulty(request.getDifficulty());
        entity.setPrepTimeMinutes(request.getPrepTimeMinutes());
        entity.setCookTimeMinutes(request.getCookTimeMinutes());
        entity.setServings(request.getServings());
        entity.setRecipeStatus(request.getRecipeStatus());
        return entity;
    }
}
