package com.manhnv.capstonebakerai.recipe.dto.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeBookmarkRequest {
    @NotNull(message = "recipeId is required")
    private UUID recipeId;
}
