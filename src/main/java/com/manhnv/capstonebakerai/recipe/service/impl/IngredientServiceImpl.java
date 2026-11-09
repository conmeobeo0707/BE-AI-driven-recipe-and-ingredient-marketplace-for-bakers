package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.IngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.IngredientResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.helper.IngredientCatalogBindingHelper;
import com.manhnv.capstonebakerai.recipe.mapper.IngredientMapper;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository repository;
    private final RecipeRepository recipeRepository;
    private final IngredientCatalogBindingHelper catalogBindingHelper;

    @Override
    public PageResponse<IngredientResponse> getAll(String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        Page<Ingredient> page = (search != null && !search.isBlank())
                ? repository.findByNameContainingIgnoreCase(search.trim(), validatedPageable)
                : repository.findAll(validatedPageable);

        List<IngredientResponse> list = page.map(IngredientMapper::toDTO).toList();


        return PageResponse.<IngredientResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    public IngredientResponse getById(UUID id) {
        Ingredient entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));
        return IngredientMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        Ingredient entity = IngredientMapper.toEntity(request);
        entity.setRecipe(recipe);

        catalogBindingHelper.bind(entity, request.getName());

        return IngredientMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public IngredientResponse update(UUID id, IngredientRequest request) {
        Ingredient existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        existing.setRecipe(recipe);
        existing.setName(request.getName());
        existing.setUnit(request.getUnit());
        existing.setQuantity(request.getQuantity());

        catalogBindingHelper.bind(existing, request.getName());

        return IngredientMapper.toDTO(repository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Ingredient not found");
        }
        repository.deleteById(id);
    }

    @Override
    public PageResponse<IngredientResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        Page<Ingredient> page = (search != null && !search.isBlank())
                ? repository.findByRecipe_IdAndNameContainingIgnoreCase(recipeId, search.trim(), validatedPageable)
                : repository.findByRecipe_Id(recipeId, validatedPageable);

        List<IngredientResponse> list = page.map(IngredientMapper::toDTO).toList();

        return PageResponse.<IngredientResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }
}
