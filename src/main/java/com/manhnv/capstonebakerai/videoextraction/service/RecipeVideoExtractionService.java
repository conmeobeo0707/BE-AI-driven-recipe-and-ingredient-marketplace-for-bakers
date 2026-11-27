package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.videoextraction.dto.RecipeVideoExtractionResponse;

import java.util.UUID;

public interface RecipeVideoExtractionService {

    RecipeResponse extractAndSave(UUID mediaId);

    RecipeVideoExtractionResponse preview(UUID mediaId);
}
