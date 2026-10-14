package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;
import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepRequest {
    private UUID recipeId;
    @Schema(example = "1")
    private Integer stepNumber;
    private String instruction;
    private List<UUID> imageIds;
}
