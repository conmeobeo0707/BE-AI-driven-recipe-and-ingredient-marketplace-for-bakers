package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeToolMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeToolService;
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
public class RecipeToolServiceImpl implements RecipeToolService {

    private final RecipeToolRepository repository;
    private final RecipeRepository recipeRepository;
    private final ToolRepository toolRepository;
    private final RecipeStepResourceHelper stepResourceHelper;

    @Override
    @Transactional(readOnly = true)
    public List<RecipeToolResponse> getAll() {
        return repository.findAll().stream().map(RecipeToolMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeToolResponse getById(UUID id) {
        return RecipeToolMapper.toDTO(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipeToolResponse> getByRecipe(UUID recipeId) {
        return repository.findByRecipe_Id(recipeId).stream().map(RecipeToolMapper::toDTO).toList();
    }

    @Override
    @Transactional
    public RecipeToolResponse create(RecipeToolRequest request) {
        if (repository.existsByRecipe_IdAndTool_Id(request.getRecipeId(), request.getToolId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tool already exists in recipe");
        }
        RecipeTool entity = RecipeToolMapper.toEntity(request);
        apply(entity, request);
        return RecipeToolMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public RecipeToolResponse update(UUID id, RecipeToolRequest request) {
        RecipeTool entity = find(id);
        UUID oldRecipeId = entity.getRecipe().getId();
        UUID oldToolId = entity.getTool().getId();
        boolean pairChanged = !oldRecipeId.equals(request.getRecipeId())
                || !oldToolId.equals(request.getToolId());
        if (pairChanged) {
            stepResourceHelper.requireToolUnusedBySteps(oldRecipeId, oldToolId);
            if (repository.existsByRecipe_IdAndTool_Id(request.getRecipeId(), request.getToolId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tool already exists in recipe");
            }
        }
        stepResourceHelper.requireToolQuantityCoversSteps(
                request.getRecipeId(), request.getToolId(), request.getQuantity());
        apply(entity, request);
        return RecipeToolMapper.toDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        RecipeTool entity = find(id);
        stepResourceHelper.requireToolUnusedBySteps(entity.getRecipe().getId(), entity.getTool().getId());
        repository.delete(entity);
    }

    private RecipeTool find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeTool not found"));
    }

    private void apply(RecipeTool entity, RecipeToolRequest request) {
        entity.setRecipe(recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found")));
        entity.setTool(toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new RuntimeException("Tool not found")));
        entity.setQuantity(request.getQuantity());
    }
}
