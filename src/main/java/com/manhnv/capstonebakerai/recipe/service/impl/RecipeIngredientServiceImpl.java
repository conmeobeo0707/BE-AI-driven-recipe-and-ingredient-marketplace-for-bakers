package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeIngredientResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeIngredientMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeIngredientService;
import com.manhnv.capstonebakerai.recipe.helper.RecipeStepResourceHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeIngredientServiceImpl implements RecipeIngredientService {

    private final RecipeIngredientRepository repository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeStepResourceHelper stepResourceHelper;

    @Override
    @Transactional(readOnly = true)
    public List<RecipeIngredientResponse> getAll() {
        return repository.findAll().stream().map(RecipeIngredientMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeIngredientResponse getById(UUID id) {
        return RecipeIngredientMapper.toDTO(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeIngredientResponse> getByRecipe(UUID recipeId) {
        return repository.findByRecipe_Id(recipeId).stream().map(RecipeIngredientMapper::toDTO).toList();
    }

    @Override
    @Transactional
    public RecipeIngredientResponse create(RecipeIngredientRequest request) {
        if (repository.existsByRecipe_IdAndIngredient_Id(request.getRecipeId(), request.getIngredientId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient already exists in recipe");
        }
        RecipeIngredient entity = RecipeIngredientMapper.toEntity(request);
        apply(entity, request);
        return RecipeIngredientMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public RecipeIngredientResponse update(UUID id, RecipeIngredientRequest request) {
        RecipeIngredient entity = find(id);
        UUID oldRecipeId = entity.getRecipe().getId();
        UUID oldIngredientId = entity.getIngredient().getId();
        boolean pairChanged = !oldRecipeId.equals(request.getRecipeId())
                || !oldIngredientId.equals(request.getIngredientId());
        if (pairChanged) {
            stepResourceHelper.requireIngredientUnusedBySteps(oldRecipeId, oldIngredientId);
            if (repository.existsByRecipe_IdAndIngredient_Id(request.getRecipeId(), request.getIngredientId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingredient already exists in recipe");
            }
        }
        stepResourceHelper.requireIngredientQuantityCoversSteps(
                request.getRecipeId(), request.getIngredientId(), request.getQuantity());
        apply(entity, request);
        return RecipeIngredientMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        RecipeIngredient entity = find(id);
        stepResourceHelper.requireIngredientUnusedBySteps(entity.getRecipe().getId(), entity.getIngredient().getId());
        repository.delete(entity);
    }

    private RecipeIngredient find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeIngredient not found"));
    }

    private void apply(RecipeIngredient entity, RecipeIngredientRequest request) {
        entity.setRecipe(recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found")));
        entity.setIngredient(ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new RuntimeException("Ingredient not found")));
        entity.setQuantity(request.getQuantity());
    }
}
