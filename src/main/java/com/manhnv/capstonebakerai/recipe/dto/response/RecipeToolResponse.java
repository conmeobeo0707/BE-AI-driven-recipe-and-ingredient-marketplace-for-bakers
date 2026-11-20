package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeToolResponse {
    private UUID id;
    private UUID recipeId;
    private UUID toolId;
    private String toolName;
    private BigDecimal quantity;
}
