package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private Integer stepNumber;
    private String instruction;
    private List<UUID> imageIds;
}
