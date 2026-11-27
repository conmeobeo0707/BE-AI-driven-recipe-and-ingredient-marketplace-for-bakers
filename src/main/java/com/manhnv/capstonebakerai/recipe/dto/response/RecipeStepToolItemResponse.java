package com.manhnv.capstonebakerai.recipe.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepToolItemResponse {
    @Schema(description = "id dòng dụng cụ của step (recipe step tool)")
    private UUID id;
    @Schema(description = "id dòng dụng cụ của recipe, trùng với RecipeResponse.tools[].id")
    private UUID recipeToolId;
    private UUID toolId;
    private String toolName;
    private String rawName;
    private BigDecimal quantity;
    private String note;
}
