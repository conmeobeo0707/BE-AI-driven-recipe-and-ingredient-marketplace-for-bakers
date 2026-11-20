package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeToolResponse;

import java.util.List;
import java.util.UUID;

public interface RecipeToolService {
    List<RecipeToolResponse> getAll();
    RecipeToolResponse getById(UUID id);
    List<RecipeToolResponse> getByRecipe(UUID recipeId);
    RecipeToolResponse create(RecipeToolRequest request);
    RecipeToolResponse update(UUID id, RecipeToolRequest request);
    void delete(UUID id);
}
