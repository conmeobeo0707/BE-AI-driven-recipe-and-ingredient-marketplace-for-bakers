package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepToolResponse {
    private UUID id;
    private UUID recipeStepId;
    private UUID toolId;
    private String toolName;
    private BigDecimal quantity;
}
