package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepResponse;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.Media;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepImage;
import com.manhnv.capstonebakerai.recipe.enums.MediaStatus;
import com.manhnv.capstonebakerai.recipe.enums.MediaType;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeStepMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.MediaRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepImageRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepService;
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
    private final MediaRepository mediaRepository;
    private final RecipeStepImageRepository recipeStepImageRepository;
    private final RecipeServiceImpl recipeService;

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

        validateContinuousStepNumbers(allStepNumbers);

        List<UUID> imageIds = request.getImageIds() == null ? List.of() : request.getImageIds();
        validateUniqueMediaIds(imageIds);
        Map<UUID, Media> mediaById = loadMediaMap(imageIds);

        RecipeStep entity = RecipeStepMapper.toEntity(request);
        entity.setRecipe(recipe);

        RecipeStep saved = recipeStepRepository.save(entity);
        if (request.getImageIds() != null) recipeService.saveStepImages(saved, imageIds, mediaById, Set.of());
        return RecipeStepMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public RecipeStepResponse updateRecipeStep(UUID id, RecipeStepRequest request) {
        RecipeStep existing = recipeStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecipeStep not found"));

        List<RecipeStepImage> existingImages = recipeStepImageRepository.findByRecipeStepIdWithMedia(id);
        Set<UUID> currentlyAttachedMediaIds = existingImages.stream()
                .map(link -> link.getMedia().getId())
                .collect(Collectors.toSet());
        List<UUID> requestedImageIds = request.getImageIds() == null ? List.of() : request.getImageIds();
        Set<UUID> allMediaIds = new HashSet<>(currentlyAttachedMediaIds);
        allMediaIds.addAll(requestedImageIds);
        Map<UUID, Media> mediaById = loadMediaMap(allMediaIds);

        Recipe recipe = recipeRepository.findById(request.getRecipeId())
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        List<RecipeStep> existingSteps = recipeStepRepository.findByRecipe_IdOrderByStepNumberAsc(request.getRecipeId());

        List<Integer> allStepNumbers = existingSteps.stream()
                .filter(step -> !step.getId().equals(id))
                .map(RecipeStep::getStepNumber)
                .collect(Collectors.toCollection(ArrayList::new));

        if (allStepNumbers.contains(request.getStepNumber())) {
            throw new RuntimeException("Step number already exists in recipe");
        }

        allStepNumbers.add(request.getStepNumber());

        validateContinuousStepNumbers(allStepNumbers);

        existing.setRecipe(recipe);
        existing.setStepNumber(request.getStepNumber());
        existing.setInstruction(request.getInstruction());
        if (request.getImageIds() != null) {
            validateUniqueMediaIds(requestedImageIds);
            recipeStepImageRepository.deleteByRecipeStepIds(List.of(id));
            recipeStepImageRepository.flush();
            recipeService.saveStepImages(existing, requestedImageIds, mediaById, currentlyAttachedMediaIds);
            Set<UUID> retainedMediaIds = new HashSet<>(requestedImageIds);
            for (UUID oldMediaId : currentlyAttachedMediaIds) {
                if (!retainedMediaIds.contains(oldMediaId)) {
                    Media detachedMedia = mediaById.get(oldMediaId);
                    if (detachedMedia != null) detachedMedia.setStatus(MediaStatus.TEMP);
                }
            }
        }

        recipeStepRepository.save(existing);

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

        List<RecipeStepImage> linkedImages = recipeStepImageRepository.findByRecipeStepIdWithMedia(id);

        List<RecipeStep> existingSteps = recipeStepRepository.findByRecipe_IdOrderByStepNumberAsc(existing.getRecipe().getId());

        List<Integer> remainingStepNumbers = existingSteps.stream()
                .filter(step -> !step.getId().equals(id))
                .map(RecipeStep::getStepNumber)
                .toList();

        if (!remainingStepNumbers.isEmpty()) {
            validateContinuousStepNumbers(remainingStepNumbers);
        }

        linkedImages.forEach(link -> link.getMedia().setStatus(MediaStatus.TEMP));
        recipeStepRepository.deleteById(id);
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

    private void validateContinuousStepNumbers(List<Integer> stepNumbers) {
        if (stepNumbers == null || stepNumbers.isEmpty()) {
            return;
        }

        Set<Integer> unique = new HashSet<>(stepNumbers);
        if (unique.size() != stepNumbers.size()) {
            throw new RuntimeException("Step number already exists in recipe");
        }

        int max = unique.stream().mapToInt(Integer::intValue).max().orElse(0);

        for (int i = 1; i <= max; i++) {
            if (!unique.contains(i)) {
                throw new RuntimeException("Step number must be continuous starting from 1");
            }
        }
    }

    private Map<UUID, Media> loadMediaMap(java.util.Collection<UUID> requestedIds) {
        if (requestedIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Media IDs cannot contain null values");
        }
        Set<UUID> uniqueIds = new HashSet<>(requestedIds);
        Map<UUID, Media> mediaById = new HashMap<>();
        for (Media media : mediaRepository.findAllById(uniqueIds)) {
            mediaById.put(media.getId(), media);
        }
        if (mediaById.size() != uniqueIds.size()) {
            Set<UUID> missingIds = new HashSet<>(uniqueIds);
            missingIds.removeAll(mediaById.keySet());
            throw new IllegalArgumentException("Media not found: " + missingIds);
        }
        return mediaById;
    }

    private void validateUniqueMediaIds(List<UUID> mediaIds) {
        if (mediaIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Media IDs cannot contain null values");
        }
        if (new HashSet<>(mediaIds).size() != mediaIds.size()) {
            throw new IllegalArgumentException("Media IDs cannot contain duplicates");
        }
    }
}
