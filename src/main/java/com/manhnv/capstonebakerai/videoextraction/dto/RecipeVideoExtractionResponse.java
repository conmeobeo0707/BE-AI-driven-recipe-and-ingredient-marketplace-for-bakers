package com.manhnv.capstonebakerai.videoextraction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"mediaId", "isRecipe", "title", "description", "difficulty",
        "prepTimeMinutes", "cookTimeMinutes", "servings", "category", "ingredients", "tools", "steps"})
public record RecipeVideoExtractionResponse(
        UUID mediaId,
        @JsonProperty("isRecipe")
        @Schema(description = "false khi video không phải hướng dẫn làm món; khi đó các field khác null và list rỗng")
        boolean isRecipe,
        @Schema(nullable = true) String title,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) DifficultyEnum difficulty,
        @Schema(nullable = true) Integer prepTimeMinutes,
        @Schema(nullable = true) Integer cookTimeMinutes,
        @Schema(nullable = true) Integer servings,
        @Schema(nullable = true) Category category,
        List<Ingredient> ingredients,
        List<Tool> tools,
        List<Step> steps
) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"rawName", "categoryId", "matched", "matchedName"})
    public record Category(
            @Schema(nullable = true, description = "Tên category AI trả về", example = "Cake") String rawName,
            @Schema(nullable = true, description = "Category khớp, hoặc category \"Khác\" khi không khớp") UUID categoryId,
            boolean matched,
            @Schema(nullable = true, description = "Tên category trong DB ứng với categoryId") String matchedName
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"key", "rawName", "quantity", "unit", "note", "matched", "ingredientId", "matchedName"})
    public record Ingredient(
            @Schema(example = "i1") String key,
            @Schema(example = "bánh Oreo") String rawName,
            @Schema(nullable = true) BigDecimal quantity,
            @Schema(nullable = true) String unit,
            @Schema(nullable = true) String note,
            boolean matched,
            @Schema(nullable = true) UUID ingredientId,
            @Schema(nullable = true) String matchedName
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"key", "rawName", "quantity", "note", "matched", "toolId", "matchedName"})
    public record Tool(
            @Schema(example = "t1") String key,
            @Schema(example = "phới lồng") String rawName,
            @Schema(nullable = true) BigDecimal quantity,
            @Schema(nullable = true) String note,
            boolean matched,
            @Schema(nullable = true) UUID toolId,
            @Schema(nullable = true) String matchedName
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"stepNumber", "instruction", "durationMinutes", "notes", "recipeStepType", "tools", "ingredients"})
    public record Step(
            Integer stepNumber,
            String instruction,
            @Schema(nullable = true) Integer durationMinutes,
            @Schema(nullable = true) String notes,
            RecipeStepType recipeStepType,
            List<ItemRef> tools,
            List<ItemRef> ingredients
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonPropertyOrder({"key", "quantity"})
    public record ItemRef(
            @Schema(description = "key của tool/ingredient ở cấp recipe", example = "i1") String key,
            @Schema(nullable = true, description = "Lượng dùng trong bước này") BigDecimal quantity
    ) {
    }
}
