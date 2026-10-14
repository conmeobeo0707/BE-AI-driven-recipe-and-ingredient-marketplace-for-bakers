package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRatingRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeRatingResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecipeRatingService {
    RecipeRatingResponse getById(UUID id);
    RecipeRatingResponse create(RecipeRatingRequest request);
    RecipeRatingResponse update(UUID id, RecipeRatingRequest request);
    void delete(UUID id);

    PageResponse<RecipeRatingResponse> getAll(String search, Pageable pageable);
    PageResponse<RecipeRatingResponse> getByCustomerId(UUID customerId, String search, Pageable pageable);
    PageResponse<RecipeRatingResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable);
}