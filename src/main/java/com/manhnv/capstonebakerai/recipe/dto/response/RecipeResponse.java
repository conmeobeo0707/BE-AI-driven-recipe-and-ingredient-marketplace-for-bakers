package com.manhnv.capstonebakerai.recipe.dto.response;

import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {
    private UUID id;
    private UUID userId;
    private UUID categoryId;
    private String title;
    private String description;
    private UUID videoId;
    private String videoUrl;
    private List<UUID> imageIds;
    private List<String> imageUrls;
    private DifficultyEnum difficulty;
    private Integer prepTimeMinutes;
    private Integer cookTimeMinutes;
    private Integer servings;
    private BigDecimal averageRating;
    private RecipeStatus recipeStatus;
    private LocalDateTime createdAt;

    private List<ToolResponse> tools;
    private List<IngredientResponse> ingredients;
    private List<RecipeStepResponse> steps;
}
