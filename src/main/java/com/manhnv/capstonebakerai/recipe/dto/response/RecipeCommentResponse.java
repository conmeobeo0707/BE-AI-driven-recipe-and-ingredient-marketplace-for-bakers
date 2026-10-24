package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCommentResponse {
    private UUID id;
    private UUID recipeId;
    private UUID userId;
    private UserSummaryResponse user;
    private String content;
    private UUID parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
