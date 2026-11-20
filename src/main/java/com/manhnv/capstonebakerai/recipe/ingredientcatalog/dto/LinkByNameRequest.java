package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LinkByNameRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull UUID catalogIngredientId
) {
}
