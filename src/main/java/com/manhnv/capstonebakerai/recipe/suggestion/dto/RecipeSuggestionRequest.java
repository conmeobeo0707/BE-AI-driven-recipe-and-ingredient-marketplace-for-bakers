package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record RecipeSuggestionRequest(
        @NotEmpty @Size(max = 20) List<@NotNull UUID> catalogIngredientIds,
        @Min(1) @Max(5) Integer limit
) {
}
