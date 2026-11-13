package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientEmbeddedRequest {
    @NotNull(message = "ingredientId is required")
    private UUID ingredientId;
    @Schema(example = "2")
    @NotNull(message = "quantity is required")
    private BigDecimal quantity;
}
