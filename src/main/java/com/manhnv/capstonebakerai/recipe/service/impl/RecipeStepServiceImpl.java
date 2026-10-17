package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeStepMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepImageRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepService;
import com.manhnv.capstonebakerai.recipe.helper.RecipeMediaHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeStepServiceImpl implements RecipeStepService {

    private final RecipeStepRepository recipeStepRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeStepImageRepository recipeStepImageRepository;
    private final RecipeMediaHelper mediaHelper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeStepResponse> getAllRecipeSteps(String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        Page<RecipeStep> page = (search != null && !search.isBlank())
                ? recipeStepRepository.findByInstructionContainingIgnoreCase(search.trim(), validatedPageable)
                : recipeStepRepository.findAll(validatedPageable);

        List<RecipeStepResponse> list = page.map(RecipeStepMapper::toDTO).toList();

        return PageResponse.<RecipeStepResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeStepResponse getRecipeStepById(UUID id) {
        RecipeStep entity = recipeStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeStep not found"));
        return RecipeStepMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public RecipeStepResponse createRecipeStep(RecipeStepRequest request) {
        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        List<RecipeStep> existingSteps = recipeStepRepository.findByRecipe_IdOrderByStepNumberAsc(request.getRecipeId());

        if (existingSteps.stream().anyMatch(s -> s.getStepNumber().equals(request.getStepNumber()))) {
            throw new RuntimeException("Step number already exists in recipe");
        }

        List<Integer> allStepNumbers = existingSteps.stream()
                .map(RecipeStep::getStepNumber)
                .collect(Collectors.toCollection(ArrayList::new));
        allStepNumbers.add(request.getStepNumber());

        mediaHelper.validateContinuousStepNumbers(allStepNumbers);

        List<UUID> imageIds = request.getImageIds() == null ? List.of() : request.getImageIds();
        mediaHelper.validateUniqueMediaIds(imageIds);
        Map<UUID, Media> mediaById = mediaHelper.loadMediaMap(imageIds);

        RecipeStep entity = RecipeStepMapper.toEntity(request);
        entity.setRecipe(recipe);

        RecipeStep saved = recipeStepRepository.save(entity);
        if (request.getImageIds() != null) mediaHelper.saveStepImages(saved, imageIds, mediaById, Set.of());
        return RecipeStepMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public RecipeStepResponse updateRecipeStep(UUID id, RecipeStepEmbeddedRequest request) {
        RecipeStep existing = recipeStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeStep not found"));
        UUID recipeId = existing.getRecipe().getId();   // không lấy recipe từ request

        Set<UUID> currentlyAttachedMediaIds =
                new HashSet<>(recipeStepImageRepository.findMediaIdsByRecipeStepId(id));
        List<UUID> requestedImageIds = request.getImageIds() == null ? List.of() : request.getImageIds();
        Set<UUID> allMediaIds = new HashSet<>(currentlyAttachedMediaIds);
        allMediaIds.addAll(requestedImageIds);
        Map<UUID, Media> mediaById = mediaHelper.loadMediaMap(allMediaIds);

        List<RecipeStep> existingSteps = recipeStepRepository.findByRecipe_IdOrderByStepNumberAsc(recipeId);
        List<Integer> allStepNumbers = existingSteps.stream()
                .filter(step -> !step.getId().equals(id))
                .map(RecipeStep::getStepNumber)
                .collect(Collectors.toCollection(ArrayList::new));
        if (allStepNumbers.contains(request.getStepNumber())) {
            throw new RuntimeException("Step number already exists in recipe");
        }
        allStepNumbers.add(request.getStepNumber());
        mediaHelper.validateContinuousStepNumbers(allStepNumbers);

        existing.setStepNumber(request.getStepNumber());
        existing.setInstruction(request.getInstruction());

        mediaHelper.validateUniqueMediaIds(requestedImageIds);
        recipeStepImageRepository.deleteByRecipeStepIds(List.of(id));
        recipeStepImageRepository.flush();
        mediaHelper.saveStepImages(existing, requestedImageIds, mediaById, currentlyAttachedMediaIds);

        List<UUID> detachedIds = new ArrayList<>();
        Set<UUID> retained = new HashSet<>(requestedImageIds);
        List<Media> detached = new ArrayList<>();
        for (UUID oldId : currentlyAttachedMediaIds) {
            if (!retained.contains(oldId) && mediaById.get(oldId) != null) {
                detached.add(mediaById.get(oldId));
                detachedIds.add(oldId);
            }
        }
        mediaHelper.detachMedia(detached);

        recipeStepRepository.save(existing);
        mediaHelper.deleteMediaAfterCommit(detachedIds);

        entityManager.flush();
        entityManager.clear();
        return RecipeStepMapper.toDTO(recipeStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeStep not found")));
    }

    @Override
    @Transactional
    public void deleteRecipeStep(UUID id) {
        RecipeStep existing = recipeStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeStep not found"));

        List<UUID> mediaIds = recipeStepImageRepository.findMediaIdsByRecipeStepId(id);

        List<RecipeStep> existingSteps =
                recipeStepRepository.findByRecipe_IdOrderByStepNumberAsc(existing.getRecipe().getId());
        List<Integer> remaining = existingSteps.stream()
                .filter(step -> !step.getId().equals(id))
                .map(RecipeStep::getStepNumber)
                .toList();
        if (!remaining.isEmpty()) mediaHelper.validateContinuousStepNumbers(remaining);

        if (!mediaIds.isEmpty()) {
            mediaHelper.detachMedia(mediaHelper.loadMediaMap(mediaIds).values());
        }
        recipeStepRepository.delete(existing);   // cascade xóa RecipeStepImage

        mediaHelper.deleteMediaAfterCommit(mediaIds);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeStepResponse> getByRecipeId(UUID recipeId, String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        Page<RecipeStep> page = (search != null && !search.isBlank())
                ? recipeStepRepository.findByRecipe_IdAndInstructionContainingIgnoreCase(recipeId, search.trim(), validatedPageable)
                : recipeStepRepository.findByRecipe_Id(recipeId, validatedPageable);

        List<RecipeStepResponse> list = page.map(RecipeStepMapper::toDTO).toList();

        return PageResponse.<RecipeStepResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

}
