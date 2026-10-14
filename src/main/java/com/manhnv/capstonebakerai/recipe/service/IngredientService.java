package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.IngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.IngredientResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;


import java.util.UUID;

public interface IngredientService {
    IngredientResponse getById(UUID id);
    IngredientResponse create(IngredientRequest request);
    IngredientResponse update(UUID id, IngredientRequest request);
    void delete(UUID id);

    PageResponse<IngredientResponse> getAll(String search, Pageable pageable);
    PageResponse<IngredientResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable);
}