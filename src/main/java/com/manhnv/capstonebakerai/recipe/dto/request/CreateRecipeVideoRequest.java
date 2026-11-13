package com.manhnv.capstonebakerai.recipe.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateRecipeVideoRequest {
    @NotNull(message = "recipeId is required")
    private UUID recipeId;
}
