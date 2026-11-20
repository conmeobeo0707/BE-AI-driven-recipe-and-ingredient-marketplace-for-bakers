package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientResponse {
    private UUID id;
    private UUID recipeId;
    private UUID ingredientId;
    private String ingredientName;
    private BigDecimal quantity;
    private String unit;
}
