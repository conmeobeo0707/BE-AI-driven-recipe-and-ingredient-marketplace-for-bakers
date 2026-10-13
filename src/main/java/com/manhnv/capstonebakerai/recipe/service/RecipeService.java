package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecipeService {
    RecipeResponse getById(UUID id);
    RecipeResponse create(RecipeRequest request);
    RecipeResponse update(UUID id, RecipeRequest request);
    void delete(UUID id);

    PageResponse<RecipeResponse> search(
            UUID userId,
            UUID categoryId,
            String search,
            DifficultyEnum difficulty,
            RecipeStatus recipeStatus,
            Pageable pageable
    );
}
