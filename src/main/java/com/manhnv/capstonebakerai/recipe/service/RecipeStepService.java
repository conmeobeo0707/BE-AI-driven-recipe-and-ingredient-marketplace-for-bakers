package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepResponse;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface RecipeStepService {
    RecipeStepResponse getRecipeStepById(UUID id);
    RecipeStepResponse createRecipeStep(RecipeStepRequest request);
    RecipeStepResponse updateRecipeStep(UUID id, RecipeStepRequest request);
    void deleteRecipeStep(UUID id);
    PageResponse<RecipeStepResponse> getAllRecipeSteps(String search, Pageable pageable);
    PageResponse<RecipeStepResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable);
}
