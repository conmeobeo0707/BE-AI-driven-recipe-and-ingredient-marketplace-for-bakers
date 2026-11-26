package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepToolEmbeddedRequest {
    @Schema(description = "id của dòng recipe tool (RecipeResponse.tools[].id)")
    @NotNull(message = "recipeToolId is required")
    private UUID recipeToolId;
    @Schema(example = "1")
    @NotNull(message = "quantity is required")
    private BigDecimal quantity;
    private String note;

    public StepToolEmbeddedRequest(UUID recipeToolId, BigDecimal quantity) {
        this(recipeToolId, quantity, null);
    }
}
