package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeBookmarkResponse {
    private UUID id;
    private UUID recipeId;
    private UUID customerId;
    private LocalDateTime createdAt;
}