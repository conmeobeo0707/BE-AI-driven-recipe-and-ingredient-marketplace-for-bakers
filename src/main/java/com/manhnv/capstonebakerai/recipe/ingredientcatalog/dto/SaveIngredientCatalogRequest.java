package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveIngredientCatalogRequest(
        @NotBlank @Size(max = 150) String canonicalName
) {
}
