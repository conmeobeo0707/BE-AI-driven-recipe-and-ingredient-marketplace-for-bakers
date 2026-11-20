package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepIngredientResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeStepIngredientMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepIngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeStepIngredientServiceImpl implements RecipeStepIngredientService {

    private final RecipeStepIngredientRepository repository;
    private final RecipeStepRepository recipeStepRepository;
    private final IngredientRepository ingredientRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecipeStepIngredientResponse> getAll() {
        return repository.findAll().stream().map(RecipeStepIngredientMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeStepIngredientResponse getById(UUID id) {
        return RecipeStepIngredientMapper.toDTO(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeStepIngredientResponse> getByRecipeStep(UUID recipeStepId) {
        return repository.findByRecipeStep_Id(recipeStepId).stream().map(RecipeStepIngredientMapper::toDTO).toList();
    }

    @Override
    @Transactional
    public RecipeStepIngredientResponse create(RecipeStepIngredientRequest request) {
        RecipeStepIngredient entity = RecipeStepIngredientMapper.toEntity(request);
        apply(entity, request);
        return RecipeStepIngredientMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public RecipeStepIngredientResponse update(UUID id, RecipeStepIngredientRequest request) {
        RecipeStepIngredient entity = find(id);
        apply(entity, request);
        return RecipeStepIngredientMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private RecipeStepIngredient find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeStepIngredient not found"));
    }

    private void apply(RecipeStepIngredient entity, RecipeStepIngredientRequest request) {
        entity.setRecipeStep(recipeStepRepository.findById(request.getRecipeStepId())
                .orElseThrow(() -> new RuntimeException("RecipeStep not found")));
        entity.setIngredient(ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Ingredient not found")));
        entity.setQuantity(request.getQuantity());
    }
}
