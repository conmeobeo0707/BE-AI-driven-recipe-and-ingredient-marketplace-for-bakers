package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepIngredientResponse {
    private UUID id;
    private UUID recipeStepId;
    private UUID recipeIngredientId;
    private UUID ingredientId;
    private String ingredientName;
    private String rawName;
    private BigDecimal quantity;
    private String unit;
}
