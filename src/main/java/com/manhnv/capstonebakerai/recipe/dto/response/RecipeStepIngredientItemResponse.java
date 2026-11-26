package com.manhnv.capstonebakerai.recipe.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepIngredientItemResponse {
    @Schema(description = "id dòng nguyên liệu của step (recipe step ingredient)")
    private UUID id;
    @Schema(description = "id dòng nguyên liệu của recipe, trùng với RecipeResponse.ingredients[].id")
    private UUID recipeIngredientId;
    private UUID ingredientId;
    private String ingredientName;
    private String rawName;
    private BigDecimal quantity;
    private String unit;
    private String note;
}
