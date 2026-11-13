package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeIngredientResponse;

import java.util.List;
import java.util.UUID;

public interface RecipeIngredientService {
    List<RecipeIngredientResponse> getAll();
    RecipeIngredientResponse getById(UUID id);
    List<RecipeIngredientResponse> getByRecipe(UUID recipeId);
    RecipeIngredientResponse create(RecipeIngredientRequest request);
    RecipeIngredientResponse update(UUID id, RecipeIngredientRequest request);
    void delete(UUID id);
}
