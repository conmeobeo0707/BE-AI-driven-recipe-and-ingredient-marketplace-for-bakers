package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeInteractionResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeInteraction;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.exception.RecipeInteractionErrorCode;
import com.manhnv.capstonebakerai.recipe.repository.RecipeBookmarkRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeInteractionRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRatingRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeInteractionService;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionDeletedEvent;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionRecordedEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeInteractionServiceImpl
        implements RecipeInteractionService {

    // The same recipe viewed again within this window (reloads, going back) is recorded once.
    private static final Duration REPEAT_WINDOW = Duration.ofMinutes(30);

    private static final List<RecipeInteractionType> STATEFUL_TYPES =
            List.of(RecipeInteractionType.BOOKMARK, RecipeInteractionType.RATING);

    private final RecipeInteractionRepository repository;
    private final CustomerRepository customerRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeBookmarkRepository bookmarkRepository;
    private final RecipeRatingRepository ratingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void record(
            UUID customerId,
            UUID recipeId,
            RecipeInteractionType type,
            Integer ratingScore,
            String recommId
    ) {
        // A recommId means the interaction came from a recommendation list; blank counts as none.
        String cleanRecommId = recommId == null || recommId.isBlank() ? null : recommId.trim();

        RecipeInteraction event = new RecipeInteraction();
        event.setCustomer(customerRepository.getReferenceById(customerId));
        event.setRecipe(recipeRepository.getReferenceById(recipeId));
        event.setInteractionType(type);
        event.setRatingScore(
                type == RecipeInteractionType.RATING
                        ? ratingScore
                        : null
        );
        event.setOccurredAt(Instant.now());
        event.setRecommId(cleanRecommId);
        RecipeInteraction saved = repository.save(event);
        eventPublisher.publishEvent(new RecipeInteractionRecordedEvent(
                saved.getId(), customerId, recipeId, type, saved.getRatingScore(), cleanRecommId,
                saved.getOccurredAt()));
    }

    @Override
    @Transactional
    public void recordViewForCurrentCustomer(UUID recipeId, String recommId) {
        Customer customer = getCurrentCustomer();
        Recipe recipe = recipeRepository.findById(recipeId)
                .filter(r -> r.getRecipeStatus() == RecipeStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(RecipeInteractionErrorCode.RECIPE_NOT_FOUND));

        if (repository.existsByCustomer_IdAndRecipe_IdAndInteractionTypeAndOccurredAtAfter(
                customer.getId(), recipe.getId(), RecipeInteractionType.VIEW, Instant.now().minus(REPEAT_WINDOW))) {
            return;
        }
        record(customer.getId(), recipe.getId(), RecipeInteractionType.VIEW, null, recommId);
    }

    @Override
    @Transactional
    public void remove(UUID customerId, UUID recipeId, RecipeInteractionType type) {
        // History rows are kept as a log; the bookmark/rating tables hold the current state.
        if (type == RecipeInteractionType.BOOKMARK || type == RecipeInteractionType.RATING) {
            eventPublisher.publishEvent(new RecipeInteractionDeletedEvent(customerId, recipeId, type));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeInteractionResponse> getCurrentCustomerHistory(
            RecipeInteractionType type,
            Pageable pageable
    ) {
        Customer customer = getCurrentCustomer();

        Page<RecipeInteraction> page = type == null
                ? repository.findByCustomer_IdOrderByOccurredAtDesc(customer.getId(), pageable)
                : repository.findByCustomer_IdAndInteractionTypeOrderByOccurredAtDesc(
                        customer.getId(), type, pageable);

        CurrentState state = currentState(customer.getId(), page.getContent());

        return PageResponse.<RecipeInteractionResponse>builder()
                .content(page.getContent().stream().map(event -> toResponse(event, state)).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    // Which BOOKMARK/RATING rows on this page still describe the customer's current bookmark or rating.
    private CurrentState currentState(UUID customerId, List<RecipeInteraction> events) {
        Set<UUID> recipeIds = new HashSet<>();
        for (RecipeInteraction event : events) {
            if (STATEFUL_TYPES.contains(event.getInteractionType())) {
                recipeIds.add(event.getRecipe().getId());
            }
        }
        if (recipeIds.isEmpty()) {
            return new CurrentState(Set.of(), Set.of(), Set.of());
        }
        return new CurrentState(
                repository.findLatestIds(customerId, recipeIds, STATEFUL_TYPES),
                bookmarkRepository.findRecipeIdsByCustomer(customerId, recipeIds),
                ratingRepository.findRecipeIdsByCustomer(customerId, recipeIds)
        );
    }

    private Customer getCurrentCustomer() {
        return customerRepository.findByUser_UserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(RecipeInteractionErrorCode.CUSTOMER_NOT_FOUND));
    }

    private RecipeInteractionResponse toResponse(RecipeInteraction event, CurrentState state) {
        Recipe recipe = event.getRecipe();
        return RecipeInteractionResponse.builder()
                .id(event.getId())
                .type(event.getInteractionType())
                .recipeId(recipe.getId())
                .recipeTitle(recipe.getTitle())
                .ratingScore(event.getRatingScore())
                .recommId(event.getRecommId())
                .occurredAt(event.getOccurredAt())
                .active(state.isActive(event))
                .build();
    }

    private record CurrentState(Set<UUID> latestIds, Set<UUID> bookmarkedRecipeIds, Set<UUID> ratedRecipeIds) {

        // Only the newest row of a bookmark/rating counts, and only while that bookmark/rating still exists.
        Boolean isActive(RecipeInteraction event) {
            RecipeInteractionType type = event.getInteractionType();
            if (!STATEFUL_TYPES.contains(type)) {
                return null;
            }
            UUID recipeId = event.getRecipe().getId();
            boolean exists = type == RecipeInteractionType.BOOKMARK
                    ? bookmarkedRecipeIds.contains(recipeId)
                    : ratedRecipeIds.contains(recipeId);
            return exists && latestIds.contains(event.getId());
        }
    }
}
