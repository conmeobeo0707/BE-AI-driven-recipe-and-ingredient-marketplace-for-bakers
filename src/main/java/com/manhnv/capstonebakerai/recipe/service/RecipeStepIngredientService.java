package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepIngredientResponse;

import java.util.List;
import java.util.UUID;

public interface RecipeStepIngredientService {
    List<RecipeStepIngredientResponse> getAll();
    RecipeStepIngredientResponse getById(UUID id);
    List<RecipeStepIngredientResponse> getByRecipeStep(UUID recipeStepId);
    RecipeStepIngredientResponse create(RecipeStepIngredientRequest request);
    RecipeStepIngredientResponse update(UUID id, RecipeStepIngredientRequest request);
    void delete(UUID id);
}
