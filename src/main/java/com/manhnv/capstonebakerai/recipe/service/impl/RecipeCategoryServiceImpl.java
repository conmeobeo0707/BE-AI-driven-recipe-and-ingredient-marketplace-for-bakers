package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCategoryRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCategoryResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeCategoryMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeCategoryServiceImpl implements RecipeCategoryService {

    private final RecipeCategoryRepository repository;

    @Override
    public RecipeCategoryResponse getById(UUID id) {
        RecipeCategory entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeCategory not found"));
        return RecipeCategoryMapper.toDTO(entity);
    }

    @Override
    public RecipeCategoryResponse create(RecipeCategoryRequest request) {
        RecipeCategory entity = RecipeCategoryMapper.toEntity(request);
        return RecipeCategoryMapper.toDTO(repository.save(entity));
    }

    @Override
    public RecipeCategoryResponse update(UUID id, RecipeCategoryRequest request) {
        RecipeCategory existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeCategory not found"));
        existing.setName(request.getName());
        return RecipeCategoryMapper.toDTO(repository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("RecipeCategory not found");
        }
        repository.deleteById(id);
    }

    @Override
    public PageResponse<RecipeCategoryResponse> getAll(String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        Page<RecipeCategory> page = (search != null && !search.isBlank())
                ? repository.findByNameContainingIgnoreCase(search.trim(), validatedPageable)
                : repository.findAll(validatedPageable);

        List<RecipeCategoryResponse> categories = page
                .map(RecipeCategoryMapper::toDTO)
                .toList();

        return PageResponse.<RecipeCategoryResponse>builder()
                .content(categories)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }
}