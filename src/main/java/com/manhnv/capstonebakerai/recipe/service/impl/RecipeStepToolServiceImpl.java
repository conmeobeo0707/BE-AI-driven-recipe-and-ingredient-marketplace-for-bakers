package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.helper.RecipeStepResourceHelper;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeStepIngredientMapper;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeStepToolMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepToolRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeStepToolServiceImpl implements RecipeStepToolService {

    private final RecipeStepToolRepository repository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeStepResourceHelper stepResourceHelper;

    @Override
    @Transactional(readOnly = true)
    public List<RecipeStepToolResponse> getAll() {
        return repository.findAll().stream().map(RecipeStepToolMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeStepToolResponse getById(UUID id) {
        return RecipeStepToolMapper.toDTO(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeStepToolResponse> getByRecipeStep(UUID recipeStepId) {
        return repository.findByRecipeStep_Id(recipeStepId).stream().map(RecipeStepToolMapper::toDTO).toList();
    }

    @Override
    @Transactional
    public RecipeStepToolResponse create(RecipeStepToolRequest request) {
        RecipeStepTool entity = RecipeStepToolMapper.toEntity(request);
        apply(entity, request);
        return RecipeStepToolMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public RecipeStepToolResponse update(UUID id, RecipeStepToolRequest request) {
        RecipeStepTool entity = find(id);
        apply(entity, request);
        return RecipeStepToolMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private RecipeStepTool find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeStepTool not found"));
    }

    private void apply(RecipeStepTool entity, RecipeStepToolRequest request) {
        RecipeStep step = recipeStepRepository.findById(request.getRecipeStepId())
                .orElseThrow(() -> new RuntimeException("RecipeStep not found"));
        RecipeTool recipeTool = stepResourceHelper.findRecipeTool(step, request.getRecipeToolId());
        validate(entity.getId(), step, recipeTool, request.getQuantity());
        entity.setRecipeStep(step);
        entity.setRecipeTool(recipeTool);
        entity.setQuantity(request.getQuantity());
        entity.setNote(RecipeStepIngredientMapper.trimToNull(request.getNote()));
    }

    private void validate(UUID entityId, RecipeStep step, RecipeTool recipeTool, BigDecimal quantity) {
        boolean alreadyInStep = repository.findByRecipeTool_Id(recipeTool.getId()).stream()
                .filter(link -> entityId == null || !entityId.equals(link.getId()))
                .anyMatch(link -> step.getId().equals(link.getRecipeStep().getId()));
        if (alreadyInStep) {
            throw badRequest("Tool already exists in this step");
        }
        BigDecimal recipeQty = recipeTool.getQuantity() == null ? BigDecimal.ZERO : recipeTool.getQuantity();
        if (quantity.compareTo(recipeQty) > 0) {
            throw badRequest("Quantity of recipe tool " + recipeTool.getId() + " in step (" + quantity.toPlainString()
                    + ") exceeds the recipe quantity (" + recipeQty.toPlainString() + ")");
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
