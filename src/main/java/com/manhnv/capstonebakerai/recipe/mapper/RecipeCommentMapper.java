package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCommentRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCommentResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.UserSummaryResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeComment;

public class RecipeCommentMapper {

    public static RecipeCommentResponse toDTO(RecipeComment entity) {
        if (entity == null) return null;
        return RecipeCommentResponse.builder()
                .id(entity.getId())
                .recipeId(entity.getRecipe() != null ? entity.getRecipe().getId() : null)
                .userId(entity.getUser() != null ? entity.getUser().getUserId() : null)
                .user(entity.getUser() == null ? null : UserSummaryResponse.builder()
                        .id(entity.getUser().getUserId())
                        .avatar_url(entity.getUser().getAvatarUrl())
                        .full_name(entity.getUser().getFullName())
                        .build())
                .content(entity.getContent())
                .parentId(entity.getParent() != null ? entity.getParent().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static RecipeComment toEntity(RecipeCommentRequest request) {
        if (request == null) return null;
        RecipeComment entity = new RecipeComment();
        entity.setContent(request.getContent());
        return entity;
    }
}
