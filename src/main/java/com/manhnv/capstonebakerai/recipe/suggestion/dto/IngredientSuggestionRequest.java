package com.manhnv.capstonebakerai.recipe.suggestion.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record IngredientSuggestionRequest(

        @NotEmpty
        @Size(max = 20)
        List<@NotBlank @Size(max = 120) String> ingredients,

        @Min(1)
        @Max(5)
        Integer limit,

        Boolean allowPartial
) {
}
