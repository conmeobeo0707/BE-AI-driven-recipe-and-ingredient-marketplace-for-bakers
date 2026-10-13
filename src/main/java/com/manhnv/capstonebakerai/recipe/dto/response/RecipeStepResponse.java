package com.manhnv.capstonebakerai.recipe.dto.response;

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
    private List<UUID> imageIds;
    private List<String> imageUrls;
}
