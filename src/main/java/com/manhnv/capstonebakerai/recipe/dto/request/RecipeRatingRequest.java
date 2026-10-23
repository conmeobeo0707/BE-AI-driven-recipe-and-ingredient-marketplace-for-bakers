package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "Optional; may be null or omitted. Send only when this rating is for a recipe returned by the recommendation API.", nullable = true)
    @Size(max = 100, message = "recommId must not exceed 100 characters")
    private String recommId;
}
