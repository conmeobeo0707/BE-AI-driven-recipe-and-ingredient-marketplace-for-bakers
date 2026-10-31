package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientResponse {
    private UUID id;
    private UUID recipeId;
    private String name;
    private String unit;
    private BigDecimal quantity;
    private UUID catalogIngredientId;
}