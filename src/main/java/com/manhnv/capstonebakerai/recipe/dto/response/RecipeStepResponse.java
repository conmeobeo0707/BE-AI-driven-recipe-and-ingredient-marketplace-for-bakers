package com.manhnv.capstonebakerai.recipe.dto.response;

import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import lombok.*;

import java.util.UUID;
import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepResponse {
    private UUID id;
    private UUID recipeId;
    private Integer stepNumber;
    private String instruction;
    private Integer durationMinutes;
    private String notes;
    private RecipeStepType recipeStepType;
    private List<UUID> imageIds;
    private List<String> imageUrls;
    private List<RecipeStepIngredientResponse> ingredients;
    private List<RecipeStepToolResponse> tools;
}
