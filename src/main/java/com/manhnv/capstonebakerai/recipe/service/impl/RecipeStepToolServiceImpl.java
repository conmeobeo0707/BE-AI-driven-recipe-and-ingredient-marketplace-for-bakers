package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepToolResponse;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeStepToolMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeStepToolServiceImpl implements RecipeStepToolService {

    private final RecipeStepToolRepository repository;
    private final RecipeStepRepository recipeStepRepository;
    private final ToolRepository toolRepository;

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
        entity.setRecipeStep(recipeStepRepository.findById(request.getRecipeStepId())
                .orElseThrow(() -> new RuntimeException("RecipeStep not found")));
        entity.setTool(toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new RuntimeException("Tool not found")));
        entity.setQuantity(request.getQuantity());
    }
}
