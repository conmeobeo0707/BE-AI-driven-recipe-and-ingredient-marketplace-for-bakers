package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRatingRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeRatingResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeRating;
import com.manhnv.capstonebakerai.recipe.event.RatingCreatedEvent;
import com.manhnv.capstonebakerai.recipe.enums.InteractionSource;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeRatingMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRatingRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeRatingService;
import com.manhnv.capstonebakerai.recipe.service.RecipeInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeRatingServiceImpl implements RecipeRatingService {

    private final RecipeRatingRepository repository;
    private final RecipeRepository recipeRepository;
    private final CustomerRepository customerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RecipeInteractionService interactionService;

    @Override
    public PageResponse<RecipeRatingResponse> getAll(String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Integer parsedScore = null;
        if (search != null && !search.isBlank()) {
            try {
                parsedScore = Integer.parseInt(search.trim());
            } catch (NumberFormatException ignored) {
                parsedScore = null;
            }
        }

        Page<RecipeRating> page = (parsedScore != null)
                ? repository.findByScore(parsedScore, validatedPageable)
                : repository.findAll(validatedPageable);

        List<RecipeRatingResponse> list = page.getContent().stream()
                .map(RecipeRatingMapper::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<RecipeRatingResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    public RecipeRatingResponse getById(UUID id) {
        RecipeRating entity = repository.findById(id).orElseThrow(() -> new RuntimeException("RecipeRating not found"));
        return RecipeRatingMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public RecipeRatingResponse create(RecipeRatingRequest request) {
        validateScore(request.getScore());

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Customer customer = customerRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (repository.existsByRecipe_IdAndCustomer_Id(request.getRecipeId(), customer.getId())) {
            throw new RuntimeException("Rating already exists");
        }

        RecipeRating entity = new RecipeRating();
        entity.setRecipe(recipe);
        entity.setCustomer(customer);
        entity.setScore(request.getScore());

        RecipeRating saved = repository.saveAndFlush(entity);
        refreshRecipeAverageRating(recipe.getId());
        interactionService.record(
                customer.getId(), recipe.getId(), RecipeInteractionType.RATING,
                request.getScore(), request.getRecommId() == null ? InteractionSource.DIRECT : InteractionSource.RECOMMENDATION,
                request.getRecommId());

        eventPublisher.publishEvent(new RatingCreatedEvent(
                saved.getId(),
                currentUserId,
                recipe.getId(),
                recipe.getTitle(),
                recipe.getUser().getUserId(),
                request.getScore()
        ));

        return RecipeRatingMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public RecipeRatingResponse update(UUID id, RecipeRatingRequest request) {
        validateScore(request.getScore());

        RecipeRating existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeRating not found"));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Customer customer = customerRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!existing.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("RecipeRating not found");
        }

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (repository.existsByRecipe_IdAndCustomer_Id(request.getRecipeId(), customer.getId())
                && !existing.getId().equals(id)) {
            throw new RuntimeException("Rating already exists");
        }

        UUID previousRecipeId = existing.getRecipe().getId();
        existing.setRecipe(recipe);
        existing.setCustomer(customer);
        existing.setScore(request.getScore());

        RecipeRating saved = repository.saveAndFlush(existing);
        refreshRecipeAverageRating(recipe.getId());
        if (!previousRecipeId.equals(recipe.getId())) {
            refreshRecipeAverageRating(previousRecipeId);
        }
        interactionService.record(
                customer.getId(), recipe.getId(), RecipeInteractionType.RATING,
                request.getScore(), request.getRecommId() == null ? InteractionSource.DIRECT : InteractionSource.RECOMMENDATION,
                request.getRecommId());
        if (!previousRecipeId.equals(recipe.getId())) {
            interactionService.remove(customer.getId(), previousRecipeId, RecipeInteractionType.RATING);
        }
        return RecipeRatingMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        RecipeRating existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeRating not found"));
        Customer customer = customerRepository.findByUser_UserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!existing.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("RecipeRating not found");
        }
        repository.delete(existing);
        interactionService.remove(customer.getId(), existing.getRecipe().getId(), RecipeInteractionType.RATING);
    }

    @Override
    public PageResponse<RecipeRatingResponse> getByCustomerId(UUID customerId, String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Integer parsedScore = null;
        if (search != null && !search.isBlank()) {
            try {
                parsedScore = Integer.parseInt(search.trim());
            } catch (NumberFormatException ignored) {
                parsedScore = null;
            }
        }

        Page<RecipeRating> page = (parsedScore != null)
                ? repository.findByCustomer_IdAndScore(customerId, parsedScore, validatedPageable)
                : repository.findByCustomer_Id(customerId, validatedPageable);

        List<RecipeRatingResponse> list = page.getContent().stream()
                .map(RecipeRatingMapper::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<RecipeRatingResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    public PageResponse<RecipeRatingResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Integer parsedScore = null;
        if (search != null && !search.isBlank()) {
            try {
                parsedScore = Integer.parseInt(search.trim());
            } catch (NumberFormatException ignored) {
                parsedScore = null;
            }
        }

        Page<RecipeRating> page = (parsedScore != null)
                ? repository.findByRecipe_IdAndScore(recipeId, parsedScore, validatedPageable)
                : repository.findByRecipe_Id(recipeId, validatedPageable);

        List<RecipeRatingResponse> list = page.getContent().stream()
                .map(RecipeRatingMapper::toDTO)
                .collect(Collectors.toList());

        return PageResponse.<RecipeRatingResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    private void validateScore(Integer score) {
        if (score == null) {
            throw new RuntimeException("Rating score is required");
        }
        if (score < 1 || score > 5) {
            throw new RuntimeException("Rating score must be from 1 to 5");
        }
    }

    private void refreshRecipeAverageRating(UUID recipeId) {
        BigDecimal averageRating = repository.findAverageRatingByRecipeId(recipeId);
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        recipe.setAverageRating(averageRating);
        recipeRepository.save(recipe);
    }
}
