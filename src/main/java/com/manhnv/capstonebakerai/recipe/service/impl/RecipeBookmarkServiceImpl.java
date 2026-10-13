package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeBookmarkRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeBookmarkResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeBookmark;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeBookmarkMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeBookmarkRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeBookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeBookmarkServiceImpl implements RecipeBookmarkService {

    private final RecipeBookmarkRepository repository;
    private final RecipeRepository recipeRepository;
    private final CustomerRepository customerRepository;

    @Override
    public PageResponse<RecipeBookmarkResponse> getAll(Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<RecipeBookmark> page = repository.findAll(validatedPageable);

        List<RecipeBookmarkResponse> list = page.map(RecipeBookmarkMapper::toDTO).toList();

        return PageResponse.<RecipeBookmarkResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    public RecipeBookmarkResponse getById(UUID id) {
        RecipeBookmark entity = repository.findById(id).orElseThrow(() -> new RuntimeException("RecipeBookmark not found"));
        return RecipeBookmarkMapper.toDTO(entity);
    }

    @Override
    public RecipeBookmarkResponse create(RecipeBookmarkRequest request) {
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Customer customer = customerRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (repository.existsByRecipe_IdAndCustomer_Id(request.getRecipeId(), customer.getId())) {
            throw new RuntimeException("Bookmark already exists");
        }

        RecipeBookmark entity = new RecipeBookmark();
        entity.setRecipe(recipe);
        entity.setCustomer(customer);

        return RecipeBookmarkMapper.toDTO(repository.save(entity));
    }

    @Override
    public RecipeBookmarkResponse update(UUID id, RecipeBookmarkRequest request) {
        RecipeBookmark existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeBookmark not found"));

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        Customer customer = customerRepository.findByUser_UserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        existing.setRecipe(recipe);
        existing.setCustomer(customer);

        return RecipeBookmarkMapper.toDTO(repository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("RecipeBookmark not found");
        }
        repository.deleteById(id);
    }

    @Override
    public PageResponse<RecipeBookmarkResponse> getByCustomerId(UUID customerId, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<RecipeBookmark> page = repository.findByCustomer_Id(customerId, validatedPageable);

        List<RecipeBookmarkResponse> list = page.map(RecipeBookmarkMapper::toDTO).toList();

        return PageResponse.<RecipeBookmarkResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    public PageResponse<RecipeBookmarkResponse> getByRecipeId(UUID recipeId, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<RecipeBookmark> page = repository.findByRecipe_Id(recipeId, validatedPageable);

        List<RecipeBookmarkResponse> list = page.map(RecipeBookmarkMapper::toDTO).toList();

        return PageResponse.<RecipeBookmarkResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }
}