package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientRequest {
    private UUID recipeId;
    private String name;
    @Schema(example = "mg")
    private String unit;
    @Schema(example = "2")
    private BigDecimal quantity;
}