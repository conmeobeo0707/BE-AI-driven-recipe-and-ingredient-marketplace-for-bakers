package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;
import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepRequest {
    @NotNull(message = "recipeId is required")
    private UUID recipeId;
    @Schema(example = "1")
    @NotNull(message = "stepNumber is required")
    private Integer stepNumber;
    @NotNull(message = "instruction is required")
    private String instruction;
    private List<UUID> imageIds;
}
