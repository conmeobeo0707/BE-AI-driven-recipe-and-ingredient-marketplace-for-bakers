package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.CreateRecipeVideoRequest;
import com.manhnv.capstonebakerai.media.event.ShotstackRenderVerifiedEvent;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeVideoRenderResponse;

public interface RecipeVideoService {
    RecipeVideoRenderResponse createVideo(CreateRecipeVideoRequest request);

    void handleRenderResult(ShotstackRenderVerifiedEvent result);
}
