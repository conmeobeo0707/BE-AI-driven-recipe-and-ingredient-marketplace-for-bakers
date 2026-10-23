package com.manhnv.capstonebakerai.recipe.dto.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeBookmarkRequest {
    @NotNull(message = "recipeId is required")
    private UUID recipeId;

    @Size(max = 100, message = "recommId must not exceed 100 characters")
    private String recommId;
}
