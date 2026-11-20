package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.CreateRecipeVideoRequest;
import com.manhnv.capstonebakerai.media.event.ShotstackRenderVerifiedEvent;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeVideoRenderResponse;

import java.util.UUID;

public interface RecipeVideoService {
    RecipeVideoRenderResponse createVideo(CreateRecipeVideoRequest request);

    void handleRenderResult(ShotstackRenderVerifiedEvent result);

    /** Cancels the in-progress render for a recipe (marks it FAILED) so a new one can be started. */
    RecipeVideoRenderResponse cancelProcessingRender(UUID recipeId);
}
