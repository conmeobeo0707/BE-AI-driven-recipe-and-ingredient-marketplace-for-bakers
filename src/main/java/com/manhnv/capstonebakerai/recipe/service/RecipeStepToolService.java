package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepToolResponse;

import java.util.List;
import java.util.UUID;

public interface RecipeStepToolService {
    List<RecipeStepToolResponse> getAll();
    RecipeStepToolResponse getById(UUID id);
    List<RecipeStepToolResponse> getByRecipeStep(UUID recipeStepId);
    RecipeStepToolResponse create(RecipeStepToolRequest request);
    RecipeStepToolResponse update(UUID id, RecipeStepToolRequest request);
    void delete(UUID id);
}
