package com.manhnv.capstonebakerai.recipe.dto.request;

import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private String title;
    @Schema(example = "Một chiếc bánh mì chảo ngon lành")
    private String description;
    private UUID videoId;
    private List<UUID> imageIds;
    private DifficultyEnum difficulty;
    @Schema(example = "3.5")
    private Integer prepTimeMinutes;
    @Schema(example = "30")
    private Integer cookTimeMinutes;
    @Schema(example = "3")
    private Integer servings;
    private RecipeStatus recipeStatus;

    private List<ToolEmbeddedRequest> tools;
    private List<IngredientEmbeddedRequest> ingredients;
    private List<RecipeStepEmbeddedRequest> steps;
}
