package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCommentRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCommentResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecipeCommentService {
    RecipeCommentResponse getById(UUID id);
    RecipeCommentResponse create(RecipeCommentRequest request);
    RecipeCommentResponse update(UUID id, RecipeCommentRequest request);
    void delete(UUID id);

    PageResponse<RecipeCommentResponse> getAll(String search, Pageable pageable);
    PageResponse<RecipeCommentResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable);
    PageResponse<RecipeCommentResponse> getByUserId(UUID userId, String search, Pageable pageable);
    PageResponse<RecipeCommentResponse> getReplies(UUID parentId, String search, Pageable pageable);
}