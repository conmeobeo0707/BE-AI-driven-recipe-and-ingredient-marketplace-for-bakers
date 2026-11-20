package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RecipeVideoRenderResponse {
    private UUID id;
    private UUID recipeId;
    private String renderId;
    private String status;
    private String videoUrl;
}
