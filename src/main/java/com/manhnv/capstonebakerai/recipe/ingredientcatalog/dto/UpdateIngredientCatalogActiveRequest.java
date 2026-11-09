package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateIngredientCatalogActiveRequest(@NotNull Boolean active) {
}
