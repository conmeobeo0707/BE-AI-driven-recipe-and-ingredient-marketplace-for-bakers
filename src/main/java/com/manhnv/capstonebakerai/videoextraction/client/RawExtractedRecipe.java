package com.manhnv.capstonebakerai.videoextraction.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawExtractedRecipe(
        @JsonProperty("isRecipe") Boolean isRecipe,
        String categoryName,
        String title,
        String description,
        String difficulty,
        Integer prepTimeMinutes,
        Integer cookTimeMinutes,
        Integer servings,
        List<Tool> tools,
        List<Ingredient> ingredients,
        List<Step> steps
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tool(String key, String name, BigDecimal quantity, String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ingredient(String key, String name, String unit, BigDecimal quantity, String note) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(
            Integer stepNumber,
            String instruction,
            Integer durationMinutes,
            String notes,
            String recipeStepType,
            List<ItemRef> tools,
            List<ItemRef> ingredients
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemRef(String key, BigDecimal quantity) {
    }
}
