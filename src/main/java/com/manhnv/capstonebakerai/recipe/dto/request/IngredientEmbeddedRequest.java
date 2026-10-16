package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientEmbeddedRequest {
    @NotNull(message = "name is required")
    private String name;
    @Schema(example = "mg")
    @NotNull(message = "unit is required")
    private String unit;
    @Schema(example = "2")
    @NotNull(message = "quantity is required")
    private BigDecimal quantity;
}
