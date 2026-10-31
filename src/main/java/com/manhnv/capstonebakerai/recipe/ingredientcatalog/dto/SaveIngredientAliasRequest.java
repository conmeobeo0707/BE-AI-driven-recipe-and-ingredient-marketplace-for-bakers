package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveIngredientAliasRequest(
        @NotBlank @Size(max = 150) String alias,
        @Size(max = 10)
        @Pattern(regexp = "[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,8})*")
        String languageCode
) {
}
