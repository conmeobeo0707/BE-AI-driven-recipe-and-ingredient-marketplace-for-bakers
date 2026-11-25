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
public class StepIngredientEmbeddedRequest {
    @Schema(description = "id của dòng recipe ingredient (RecipeResponse.ingredients[].recipeIngredientId)")
    @NotNull(message = "recipeIngredientId is required")
    private UUID recipeIngredientId;
    @Schema(example = "100")
    @NotNull(message = "quantity is required")
    private BigDecimal quantity;
}
