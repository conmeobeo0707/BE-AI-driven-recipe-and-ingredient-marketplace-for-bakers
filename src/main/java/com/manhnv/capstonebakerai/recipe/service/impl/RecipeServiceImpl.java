package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeImage;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepImage;
import com.manhnv.capstonebakerai.recipe.entity.Media;
import com.manhnv.capstonebakerai.recipe.entity.Tool;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.MediaStatus;
import com.manhnv.capstonebakerai.recipe.enums.MediaType;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeMapper;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeImageRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepImageRepository;
import com.manhnv.capstonebakerai.recipe.repository.MediaRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.exception.NotFoundException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository repository;
    private final UserRepository userRepository;
    private final RecipeCategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final ToolRepository toolRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final MediaRepository mediaRepository;
    private final RecipeImageRepository recipeImageRepository;
    private final RecipeStepImageRepository recipeStepImageRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getById(UUID id) {
        Recipe entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        return RecipeMapper.toDTO(entity);
    }

    @Override
    @Transactional
    public RecipeResponse create(RecipeRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RecipeCategory category = request.getCategoryId() == null ? null
                : categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("RecipeCategory not found"));

        validateStepNumbers(request.getSteps());

        List<UUID> requestedMediaIdList = collectRequestedMediaIds(request);
        validateUniqueMediaIds(requestedMediaIdList);
        Set<UUID> requestedMediaIds = new HashSet<>(requestedMediaIdList);
        Map<UUID, Media> mediaById = loadMediaMap(requestedMediaIds);
        Set<UUID> noCurrentlyAttachedMedia = Set.of();

        Recipe recipe = RecipeMapper.toEntity(request);
        if (request.getVideoId() != null) {
            recipe.setMedia(resolveMedia(request.getVideoId(), MediaType.VIDEO, mediaById, noCurrentlyAttachedMedia));
        }
        recipe.setUser(user);
        recipe.setRecipeCategory(category);

        Recipe savedRecipe = repository.save(recipe);
        UUID recipeId = savedRecipe.getId();

        if (request.getImageIds() != null) {
            saveRecipeImages(savedRecipe, request.getImageIds(), mediaById, noCurrentlyAttachedMedia);
        }

        if (request.getTools() != null) {
            List<Tool> tools = request.getTools().stream().map(toolRequest -> {
                Tool tool = new Tool();
                tool.setName(toolRequest.getName());
                tool.setRecipe(savedRecipe);
                return tool;
            }).collect(Collectors.toList());
            toolRepository.saveAll(tools);
        }

        if (request.getIngredients() != null) {
            List<Ingredient> ingredients = request.getIngredients().stream().map(ingredientRequest -> {
                Ingredient ingredient = new Ingredient();
                ingredient.setName(ingredientRequest.getName());
                ingredient.setUnit(ingredientRequest.getUnit());
                ingredient.setQuantity(ingredientRequest.getQuantity());
                ingredient.setRecipe(savedRecipe);
                return ingredient;
            }).collect(Collectors.toList());
            ingredientRepository.saveAll(ingredients);
        }

        if (request.getSteps() != null) {
            List<RecipeStep> steps = request.getSteps().stream().map(stepRequest -> {
                RecipeStep step = new RecipeStep();
                step.setStepNumber(stepRequest.getStepNumber());
                step.setInstruction(stepRequest.getInstruction());
                step.setRecipe(savedRecipe);
                return step;
            }).collect(Collectors.toList());
            List<RecipeStep> savedSteps = recipeStepRepository.saveAll(steps);
            for (int i = 0; i < savedSteps.size(); i++) {
                List<UUID> imageIds = request.getSteps().get(i).getImageIds();
                if (imageIds != null) saveStepImages(savedSteps.get(i), imageIds, mediaById, noCurrentlyAttachedMedia);
            }
        }

        entityManager.flush();
        entityManager.clear();

        Recipe detail = repository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        return RecipeMapper.toDTO(detail);
    }

    @Override
    @Transactional
    public RecipeResponse update(UUID recipeId, RecipeRequest request) {
        Recipe existing = repository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        List<RecipeStepEmbeddedRequest> steps = nullToEmpty(request.getSteps());
        List<UUID> imageIds = nullToEmpty(request.getImageIds());
        List<com.manhnv.capstonebakerai.recipe.dto.request.ToolEmbeddedRequest> tools = nullToEmpty(request.getTools());
        List<com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest> ingredients = nullToEmpty(request.getIngredients());

        validateStepNumbers(steps);

        List<RecipeStep> existingSteps = recipeStepRepository.findByRecipe_IdOrderByStepNumberAsc(recipeId);

        Set<UUID> currentlyAttachedMediaIds = new HashSet<>();
        if (existing.getMedia() != null) currentlyAttachedMediaIds.add(existing.getMedia().getId());
        currentlyAttachedMediaIds.addAll(recipeImageRepository.findMediaIdsByRecipeId(recipeId));
        currentlyAttachedMediaIds.addAll(recipeStepImageRepository.findMediaIdsByRecipeId(recipeId));

        List<UUID> requestedMediaIdList = collectRequestedMediaIds(request.getVideoId(), imageIds, steps);
        validateUniqueMediaIds(requestedMediaIdList);
        Set<UUID> allMediaIds = new HashSet<>(currentlyAttachedMediaIds);
        allMediaIds.addAll(requestedMediaIdList);
        Map<UUID, Media> mediaById = loadMediaMap(allMediaIds);

        Set<UUID> mediaIdsInUseAfterUpdate = new HashSet<>(requestedMediaIdList);

        RecipeCategory category = request.getCategoryId() == null ? null
                : categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("RecipeCategory not found"));
        existing.setRecipeCategory(category);

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setMedia(request.getVideoId() == null ? null
                : resolveMedia(request.getVideoId(), MediaType.VIDEO, mediaById, currentlyAttachedMediaIds));
        existing.setDifficulty(request.getDifficulty());
        existing.setPrepTimeMinutes(request.getPrepTimeMinutes());
        existing.setCookTimeMinutes(request.getCookTimeMinutes());
        existing.setServings(request.getServings());
        existing.setRecipeStatus(request.getRecipeStatus());

        repository.save(existing);

        recipeImageRepository.deleteByRecipeId(recipeId);
        recipeImageRepository.flush();
        saveRecipeImages(existing, imageIds, mediaById, currentlyAttachedMediaIds);

        // ---- Tools ----
        toolRepository.deleteByRecipeId(recipeId);
        toolRepository.flush();
        toolRepository.saveAll(tools.stream().map(toolRequest -> {
            Tool newTool = new Tool();
            newTool.setName(toolRequest.getName());
            newTool.setRecipe(existing);
            return newTool;
        }).toList());

        // ---- Ingredients ----
        ingredientRepository.deleteByRecipeId(recipeId);
        ingredientRepository.flush();
        ingredientRepository.saveAll(ingredients.stream().map(ingReq -> {
            Ingredient newIngredient = new Ingredient();
            newIngredient.setName(ingReq.getName());
            newIngredient.setUnit(ingReq.getUnit());
            newIngredient.setQuantity(ingReq.getQuantity());
            newIngredient.setRecipe(existing);
            return newIngredient;
        }).toList());

        // ---- Steps ----
        if (!existingSteps.isEmpty()) {
            recipeStepImageRepository.deleteByRecipeStepIds(existingSteps.stream().map(RecipeStep::getId).toList());
            recipeStepImageRepository.flush();
            recipeStepRepository.deleteAll(existingSteps);
            recipeStepRepository.flush();
        }
        if (!steps.isEmpty()) {
            List<RecipeStep> newSteps = steps.stream().map(stepRequest -> {
                RecipeStep newStep = new RecipeStep();
                newStep.setStepNumber(stepRequest.getStepNumber());
                newStep.setInstruction(stepRequest.getInstruction());
                newStep.setRecipe(existing);
                return newStep;
            }).toList();

            List<RecipeStep> savedSteps = recipeStepRepository.saveAll(newSteps);
            for (int i = 0; i < savedSteps.size(); i++) {
                List<UUID> stepImageIds = nullToEmpty(steps.get(i).getImageIds());
                saveStepImages(savedSteps.get(i), stepImageIds, mediaById, currentlyAttachedMediaIds);
            }
        }

        for (UUID oldMediaId : currentlyAttachedMediaIds) {
            if (!mediaIdsInUseAfterUpdate.contains(oldMediaId)) {
                Media detachedMedia = mediaById.get(oldMediaId);
                if (detachedMedia != null) detachedMedia.setStatus(MediaStatus.TEMP);
            }
        }

        entityManager.flush();
        entityManager.clear();

        Recipe detail = repository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        return RecipeMapper.toDTO(detail);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Recipe recipe = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        if (recipe.getRecipeStatus() == RecipeStatus.HIDDEN) {
            throw new RuntimeException("Recipe is already hidden");
        }

        recipe.setRecipeStatus(RecipeStatus.HIDDEN);
        repository.save(recipe);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecipeResponse> search(
            UUID userId,
            UUID categoryId,
            String search,
            DifficultyEnum difficulty,
            RecipeStatus recipeStatus,
            Pageable pageable
    ) {
        Specification<Recipe> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("userId"), userId));
            }

            if (categoryId != null) {
                predicates.add(cb.equal(root.get("recipeCategory").get("id"), categoryId));
            }

            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), keyword));
            }

            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }

            if (recipeStatus != null) {
                predicates.add(cb.equal(root.get("recipeStatus"), recipeStatus));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Recipe> page = repository.findAll(spec, pageable);

        return PageResponse.<RecipeResponse>builder()
                .content(page.getContent().stream().map(RecipeMapper::toDTO).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    private void validateStepNumbers(List<RecipeStepEmbeddedRequest> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }

        Set<Integer> stepNumbers = steps.stream()
                .map(RecipeStepEmbeddedRequest::getStepNumber)
                .collect(Collectors.toSet());

        int max = stepNumbers.stream().mapToInt(Integer::intValue).max().orElse(0);

        if (stepNumbers.size() != steps.size()) {
            throw new RuntimeException("Step number contains duplicates");
        }

        for (int i = 1; i <= max; i++) {
            if (!stepNumbers.contains(i)) {
                throw new RuntimeException("Step number must be continuous starting from 1");
            }
        }
    }

    private Media resolveMedia(UUID mediaId, MediaType expectedType, Map<UUID, Media> mediaById,
                               Set<UUID> currentlyAttachedMediaIds) {
        Media media = mediaById.get(mediaId);
        if (media == null) throw new IllegalArgumentException("Media not found: " + mediaId);
        if (media.getType() != expectedType) {
            throw new IllegalArgumentException("Media " + mediaId + " must have type " + expectedType);
        }
        if (media.getStatus() != MediaStatus.TEMP && !currentlyAttachedMediaIds.contains(mediaId)) {
            throw new IllegalArgumentException("Media " + mediaId + " is already attached to another resource");
        }
        media.setStatus(MediaStatus.ATTACHED);
        return media;
    }

    private void saveRecipeImages(Recipe recipe, List<UUID> mediaIds, Map<UUID, Media> mediaById,
                                  Set<UUID> currentlyAttachedMediaIds) {
        validateUniqueMediaIds(mediaIds);
        List<RecipeImage> links = new ArrayList<>();
        for (int index = 0; index < mediaIds.size(); index++) {
            links.add(RecipeImage.builder()
                    .recipe(recipe)
                    .media(resolveMedia(mediaIds.get(index), MediaType.IMAGE, mediaById, currentlyAttachedMediaIds))
                    .sortOrder(index + 1)
                    .build());
        }
        recipeImageRepository.saveAll(links);
    }

    public void saveStepImages(RecipeStep step, List<UUID> mediaIds, Map<UUID, Media> mediaById,
                               Set<UUID> currentlyAttachedMediaIds) {
        validateUniqueMediaIds(mediaIds);
        List<RecipeStepImage> links = new ArrayList<>();
        for (int index = 0; index < mediaIds.size(); index++) {
            links.add(RecipeStepImage.builder()
                    .recipeStep(step)
                    .media(resolveMedia(mediaIds.get(index), MediaType.IMAGE, mediaById, currentlyAttachedMediaIds))
                    .sortOrder(index + 1)
                    .build());
        }
        recipeStepImageRepository.saveAll(links);   // không gán lại vào step
    }

    private Map<UUID, Media> loadMediaMap(Set<UUID> mediaIds) {
        if (mediaIds.isEmpty()) return new java.util.HashMap<>();
        if (mediaIds.contains(null)) throw new IllegalArgumentException("Media IDs cannot contain null values");
        Map<UUID, Media> mediaById = new java.util.HashMap<>();
        for (Media media : mediaRepository.findAllById(mediaIds)) {
            mediaById.put(media.getId(), media);
        }
        if (mediaById.size() != mediaIds.size()) {
            Set<UUID> missingIds = new HashSet<>(mediaIds);
            missingIds.removeAll(mediaById.keySet());
            throw new IllegalArgumentException("Media not found: " + missingIds);
        }
        return mediaById;
    }

    private List<UUID> collectRequestedMediaIds(RecipeRequest request) {
        return collectRequestedMediaIds(request.getVideoId(), nullToEmpty(request.getImageIds()), nullToEmpty(request.getSteps()));
    }

    private List<UUID> collectRequestedMediaIds(UUID videoId, List<UUID> imageIds,
                                                List<RecipeStepEmbeddedRequest> steps) {
        List<UUID> ids = new ArrayList<>();
        if (videoId != null) ids.add(videoId);
        ids.addAll(imageIds);
        steps.stream().map(RecipeStepEmbeddedRequest::getImageIds)
                .filter(java.util.Objects::nonNull).forEach(ids::addAll);
        return ids;
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private void validateUniqueMediaIds(List<UUID> mediaIds) {
        if (mediaIds == null) return;
        if (mediaIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Media IDs cannot contain null values");
        }
        if (new HashSet<>(mediaIds).size() != mediaIds.size()) {
            throw new IllegalArgumentException("Media IDs cannot contain duplicates");
        }
    }

}
