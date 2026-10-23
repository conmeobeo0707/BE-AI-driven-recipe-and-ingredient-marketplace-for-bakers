package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.recipe.enums.InteractionSource;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeInteractionResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RecipeInteractionService {

    void record(
            UUID customerId,
            UUID recipeId,
            RecipeInteractionType type,
            Integer ratingScore,
            InteractionSource requestedSource,
            String recommId
    );

    void recordViewForCurrentCustomer(UUID recipeId, InteractionSource source, String recommId);

    void remove(UUID customerId, UUID recipeId, RecipeInteractionType type);

    void recordSearchForCurrentCustomer(String query);

    PageResponse<RecipeInteractionResponse> getCurrentCustomerHistory(
            RecipeInteractionType type,
            Pageable pageable
    );
}
