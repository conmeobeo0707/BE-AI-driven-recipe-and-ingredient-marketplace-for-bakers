package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCategoryRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCategoryResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.exception.RecipeCatalogErrorCode;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeCategoryMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
                .orElseThrow(() -> new AppException(RecipeCatalogErrorCode.CATEGORY_NOT_FOUND));
        return RecipeCategoryMapper.toDTO(entity);
    }

    @Override
    public RecipeCategoryResponse create(RecipeCategoryRequest request) {
        String name = validName(request.getName());
        if (repository.existsByNormalizedName(name)) {
            throw nameTaken(name);
        }
        RecipeCategory entity = RecipeCategoryMapper.toEntity(request);
        entity.setName(name);
        return RecipeCategoryMapper.toDTO(save(entity));
    }

    @Override
    public RecipeCategoryResponse update(UUID id, RecipeCategoryRequest request) {
        RecipeCategory existing = repository.findById(id)
                .orElseThrow(() -> new AppException(RecipeCatalogErrorCode.CATEGORY_NOT_FOUND));
        String name = validName(request.getName());
        if (repository.existsByNormalizedNameAndIdNot(name, id)) {
            throw nameTaken(name);
        }
        existing.setName(name);
        return RecipeCategoryMapper.toDTO(save(existing));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new AppException(RecipeCatalogErrorCode.CATEGORY_NOT_FOUND);
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

    private RecipeCategory save(RecipeCategory category) {
        try {
            return repository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw nameTaken(category.getName());
        }
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new AppException(RecipeCatalogErrorCode.NAME_REQUIRED);
        }
        return name.trim();
    }

    private static AppException nameTaken(String name) {
        return new AppException(RecipeCatalogErrorCode.CATEGORY_NAME_ALREADY_EXISTS, "Category '" + name + "' already exists");
    }
}
