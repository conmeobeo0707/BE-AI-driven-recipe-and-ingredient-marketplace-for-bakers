package com.manhnv.capstonebakerai.recipe.dto.request;

import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRequest {
    private UUID categoryId;
    @Schema(example = "Bánh mì chảo")
    @NotNull(message = "title is required")
    private String title;
    @Schema(example = "Một chiếc bánh mì chảo ngon lành")
    private String description;
    private UUID videoId;
    private List<UUID> imageIds;
    @NotNull(message = "difficulty is required")
    private DifficultyEnum difficulty;
    @Schema(example = "15")
    @Min(value = 0, message = "prepTimeMinutes must be >= 0")
    private Integer prepTimeMinutes;
    @Schema(example = "30")
    @Min(value = 0, message = "cookTimeMinutes must be >= 0")
    private Integer cookTimeMinutes;
    @Schema(example = "3")
    @Min(value = 1, message = "servings must be >= 1")
    private Integer servings;
    @NotNull(message = "recipeStatus is required")
    private RecipeStatus recipeStatus;

    @Valid
    private List<ToolEmbeddedRequest> tools;
    @Valid
    private List<@NotNull IngredientEmbeddedRequest> ingredients;
    @Valid
    private List<RecipeStepEmbeddedRequest> steps;
}
