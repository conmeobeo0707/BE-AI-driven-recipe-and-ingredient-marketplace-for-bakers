package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record ConfirmIngredientSuggestionRequest(
        @NotEmpty @Size(max = 20) List<@NotNull UUID> catalogIngredientIds,
        @Size(max = 20) List<@NotBlank @Size(max = 120) String> unmatchedIngredients,
        @Min(1) @Max(5) Integer limit,
        Boolean allowPartial
) {
}
