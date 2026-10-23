package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeInteractionResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeInteraction;
import com.manhnv.capstonebakerai.recipe.enums.InteractionSource;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.recipe.repository.RecipeInteractionRepository;
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

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeInteractionServiceImpl
        implements RecipeInteractionService {

    private final RecipeInteractionRepository repository;
    private final CustomerRepository customerRepository;
    private final RecipeRepository recipeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void record(
            UUID customerId,
            UUID recipeId,
            RecipeInteractionType type,
            Integer ratingScore,
            InteractionSource requestedSource,
            String recommId
    ) {
        RecipeInteraction event = new RecipeInteraction();
        event.setCustomer(customerRepository.getReferenceById(customerId));
        event.setRecipe(recipeRepository.getReferenceById(recipeId));
        event.setInteractionType(type);
        event.setRatingScore(
                type == RecipeInteractionType.RATING
                        ? ratingScore
                        : null
        );
        event.setSource(requestedSource == null ? InteractionSource.DIRECT : requestedSource);
        event.setOccurredAt(Instant.now());
        event.setRecommId(recommId);
        RecipeInteraction saved = repository.save(event);
        if (type != RecipeInteractionType.SEARCH) {
            eventPublisher.publishEvent(new RecipeInteractionRecordedEvent(
                    saved.getId(), customerId, recipeId, type, saved.getRatingScore(), recommId, saved.getOccurredAt()));
        }
    }

    @Override
    @Transactional
    public void recordViewForCurrentCustomer(UUID recipeId, InteractionSource source, String recommId) {
        Customer customer = getCurrentCustomer();
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        InteractionSource effectiveSource = recommId == null || recommId.isBlank()
                ? source : InteractionSource.RECOMMENDATION;
        record(customer.getId(), recipe.getId(), RecipeInteractionType.VIEW, null, effectiveSource, recommId);
    }

    @Override
    @Transactional
    public void remove(UUID customerId, UUID recipeId, RecipeInteractionType type) {
        if (type == RecipeInteractionType.BOOKMARK || type == RecipeInteractionType.RATING) {
            eventPublisher.publishEvent(new RecipeInteractionDeletedEvent(customerId, recipeId, type));
        }
    }

    @Override
    @Transactional
    public void recordSearchForCurrentCustomer(String query) {
        if (query == null || query.isBlank()) {
            return;
        }

        Customer customer = customerRepository.findByUser_UserId(SecurityUtils.getCurrentUserId())
                .orElse(null);
        if (customer == null) {
            return;
        }

        RecipeInteraction event = new RecipeInteraction();
        event.setCustomer(customer);
        event.setInteractionType(RecipeInteractionType.SEARCH);
        event.setSearchQuery(limitSearchQuery(query.trim()));
        event.setSource(InteractionSource.SEARCH);
        event.setOccurredAt(Instant.now());
        repository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeInteractionResponse> getCurrentCustomerHistory(
            RecipeInteractionType type,
            Pageable pageable
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Customer customer = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Page<RecipeInteraction> page = type == null
                ? repository.findByCustomer_IdOrderByOccurredAtDesc(customer.getId(), pageable)
                : repository.findByCustomer_IdAndInteractionTypeOrderByOccurredAtDesc(
                        customer.getId(), type, pageable);

        return PageResponse.<RecipeInteractionResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    private Customer getCurrentCustomer() {
        return customerRepository.findByUser_UserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    private RecipeInteractionResponse toResponse(RecipeInteraction event) {
        Recipe recipe = event.getRecipe();
        return RecipeInteractionResponse.builder()
                .id(event.getId())
                .type(event.getInteractionType())
                .recipeId(recipe == null ? null : recipe.getId())
                .recipeTitle(recipe == null ? null : recipe.getTitle())
                .ratingScore(event.getRatingScore())
                .recommId(event.getRecommId())
                .searchQuery(event.getSearchQuery())
                .source(event.getSource())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    private String limitSearchQuery(String query) {
        return query.length() <= 255 ? query : query.substring(0, 255);
    }
}
