package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepEmbeddedRequest {
    @Schema(example = "1")
    @NotNull(message = "stepNumber is required")
    private Integer stepNumber;
    @NotNull(message = "instruction is required")
    private String instruction;
    @Schema(example = "10")
    @NotNull(message = "durationMinutes is required")
    @Min(value = 0, message = "durationMinutes must be >= 0")
    private Integer durationMinutes;
    private String notes;
    @Schema(example = "PREPARATION")
    @NotNull(message = "recipeStepType is required")
    private RecipeStepType recipeStepType;
    private List<UUID> imageIds;

    @Valid
    private List<@NotNull IngredientEmbeddedRequest> ingredients;

    @Valid
    private List<@NotNull ToolEmbeddedRequest> tools;
}
