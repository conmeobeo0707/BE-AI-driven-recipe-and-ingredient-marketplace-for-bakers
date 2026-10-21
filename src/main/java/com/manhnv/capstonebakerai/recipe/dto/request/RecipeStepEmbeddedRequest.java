package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
    private List<UUID> imageIds;
}
