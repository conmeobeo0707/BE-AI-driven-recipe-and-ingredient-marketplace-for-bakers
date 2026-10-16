package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRatingRequest {
    @NotNull(message = "recipeId is required")
    private UUID recipeId;
    @Schema(example = "5")
    @NotNull(message = "score is required")
    private Integer score;
}
