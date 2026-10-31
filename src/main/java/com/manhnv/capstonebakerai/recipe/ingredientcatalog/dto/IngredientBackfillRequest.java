package com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record IngredientBackfillRequest(
        @NotEmpty @Size(max = 200) List<@NotNull @Valid Mapping> mappings
) {
    public record Mapping(
            @NotNull UUID ingredientId,
            @NotNull UUID catalogIngredientId,
            @NotBlank @Size(max = 255) String expectedName
    ) {
    }
}
