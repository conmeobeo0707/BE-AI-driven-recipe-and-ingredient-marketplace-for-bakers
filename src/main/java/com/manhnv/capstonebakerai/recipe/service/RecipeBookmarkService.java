package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeBookmarkRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeBookmarkResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecipeBookmarkService {
    RecipeBookmarkResponse getById(UUID id);
    RecipeBookmarkResponse create(RecipeBookmarkRequest request);
    RecipeBookmarkResponse update(UUID id, RecipeBookmarkRequest request);
    void delete(UUID id);

    PageResponse<RecipeBookmarkResponse> getAll(Pageable pageable);
    PageResponse<RecipeBookmarkResponse> getByCustomerId(UUID customerId, Pageable pageable);
    PageResponse<RecipeBookmarkResponse> getByRecipeId(UUID recipeId, Pageable pageable);
}