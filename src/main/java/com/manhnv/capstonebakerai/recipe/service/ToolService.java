package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.request.ToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.ToolResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ToolService {
    ToolResponse getById(UUID id);
    ToolResponse create(ToolRequest request);
    ToolResponse update(UUID id, ToolRequest request);
    void delete(UUID id);

    PageResponse<ToolResponse> getAll(String search, Pageable pageable);
    PageResponse<ToolResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable);
}