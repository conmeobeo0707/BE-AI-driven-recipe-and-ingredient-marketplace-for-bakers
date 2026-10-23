package com.manhnv.capstonebakerai.recommendation.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recommendation.dto.RecipeRecommendationResponse;
import com.recombee.api_client.RecombeeClient;
import com.recombee.api_client.api_requests.RecommendItemsToUser;
import com.recombee.api_client.bindings.Recommendation;
import com.recombee.api_client.bindings.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ObjectProvider<RecombeeClient> clientProvider;
    private final CustomerRepository customerRepository;
    private final RecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public RecipeRecommendationResponse recommend(UUID userId, int count) {
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND));

        RecombeeClient client = clientProvider.getIfAvailable();
        if (client == null) return fallback(count);

        try {
            RecommendationResponse result = client.send(
                    new RecommendItemsToUser(customer.getId().toString(), count)
                            .setFilter("'isPublished' == true")
            );

            List<UUID> ids = new ArrayList<>();
            for (Recommendation recommendation : result) {
                try {
                    ids.add(UUID.fromString(recommendation.getId()));
                } catch (IllegalArgumentException ignored) {
                    log.warn("Recombee returned a non-UUID recipe ID: {}", recommendation.getId());
                }
            }
            if (ids.isEmpty()) return fallback(count);

            Map<UUID, Recipe> recipesById = recipeRepository
                    .findByIdInAndRecipeStatus(ids, RecipeStatus.PUBLISHED).stream()
                    .collect(Collectors.toMap(Recipe::getId, recipe -> recipe));
            List<RecipeResponse> recipes = ids.stream()
                    .map(recipesById::get)
                    .filter(Objects::nonNull)
                    .map(RecipeMapper::toDTO)
                    .toList();

            if (recipes.isEmpty()) return fallback(count);
            return new RecipeRecommendationResponse("RECOMBEE", result.getRecommId(), recipes);
        } catch (Exception exception) {
            log.warn("Recombee recommendation failed; using database fallback", exception);
            return fallback(count);
        }
    }

    private RecipeRecommendationResponse fallback(int count) {
        List<RecipeResponse> recipes = recipeRepository
                .findByRecipeStatusOrderByCreatedAtDesc(RecipeStatus.PUBLISHED).stream()
                .limit(count)
                .map(RecipeMapper::toDTO)
                .toList();
        return new RecipeRecommendationResponse("FALLBACK", null, recipes);
    }
}
