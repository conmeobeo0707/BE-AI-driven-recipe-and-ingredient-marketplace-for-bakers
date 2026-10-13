package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCategoryRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCategoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecipeCategoryService {
    RecipeCategoryResponse getById(UUID id);
    RecipeCategoryResponse create(RecipeCategoryRequest request);
    RecipeCategoryResponse update(UUID id, RecipeCategoryRequest request);
    void delete(UUID id);
    PageResponse<RecipeCategoryResponse> getAll(String search, Pageable pageable);
}