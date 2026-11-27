package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepIngredientResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;
import com.manhnv.capstonebakerai.recipe.helper.RecipeStepResourceHelper;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeStepIngredientMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepIngredientService;
import lombok.RequiredArgsConstructor;
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.exception.RecipeErrorCode;
import com.manhnv.capstonebakerai.recipe.exception.RecipeStepErrors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeStepIngredientServiceImpl implements RecipeStepIngredientService {

    private final RecipeStepIngredientRepository repository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeStepResourceHelper stepResourceHelper;

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
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_STEP_INGREDIENT_NOT_FOUND));
    }

    private void apply(RecipeStepIngredient entity, RecipeStepIngredientRequest request) {
        RecipeStep step = recipeStepRepository.findById(request.getRecipeStepId())
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_STEP_NOT_FOUND));
        RecipeIngredient recipeIngredient = stepResourceHelper.findRecipeIngredient(step, request.getRecipeIngredientId());
        validateQuantity(entity.getId(), step, recipeIngredient, request.getQuantity());
        entity.setRecipeStep(step);
        entity.setRecipeIngredient(recipeIngredient);
        entity.setQuantity(request.getQuantity());
        entity.setNote(RecipeStepIngredientMapper.trimToNull(request.getNote()));
    }

    private void validateQuantity(UUID entityId, RecipeStep step, RecipeIngredient recipeIngredient, BigDecimal quantity) {
        List<RecipeStepIngredient> others = repository.findByRecipeIngredient_Id(recipeIngredient.getId()).stream()
                .filter(link -> entityId == null || !entityId.equals(link.getId()))
                .toList();
        if (others.stream().anyMatch(link -> step.getId().equals(link.getRecipeStep().getId()))) {
            throw RecipeStepErrors.ingredientDuplicateInStep(recipeIngredient.getId(), step.getStepNumber());
        }
        BigDecimal recipeQty = recipeIngredient.getQuantity() == null ? BigDecimal.ZERO : recipeIngredient.getQuantity();
        BigDecimal total = others.stream()
                .map(link -> link.getQuantity() == null ? BigDecimal.ZERO : link.getQuantity())
                .reduce(quantity, BigDecimal::add);
        if (total.compareTo(recipeQty) > 0) {
            throw RecipeStepErrors.ingredientTotalExceeds("recipeIngredientId", recipeIngredient.getId(), step.getStepNumber(), total, recipeQty);
        }
    }
}
