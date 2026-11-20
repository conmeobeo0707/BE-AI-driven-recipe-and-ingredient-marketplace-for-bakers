package com.manhnv.capstonebakerai.recipe.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepIngredientRequest {
    @NotNull
    private UUID recipeStepId;

    @NotNull
    private UUID ingredientId;

    @NotNull
    private BigDecimal quantity;
}
