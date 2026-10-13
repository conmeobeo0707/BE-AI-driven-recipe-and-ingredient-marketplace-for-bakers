package com.manhnv.capstonebakerai.recipe.dto.request;

import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCommentRequest {
    private UUID recipeId;
    private String content;
    private UUID parentId;
}