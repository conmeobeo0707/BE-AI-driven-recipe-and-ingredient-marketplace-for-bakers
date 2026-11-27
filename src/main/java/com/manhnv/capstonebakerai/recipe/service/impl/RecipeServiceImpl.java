package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.recipe.helper.RecipeMediaHelper;
import com.manhnv.capstonebakerai.recipe.helper.RecipeStepDurationHelper;
import com.manhnv.capstonebakerai.recipe.helper.RecipeStepResourceHelper;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.ToolEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeImage;
import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.recipe.entity.Tool;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientAliasService;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.media.enums.MediaType;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeMapper;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeImageRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepImageRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeToolRepository;
import com.manhnv.capstonebakerai.recipe.service.RecipeService;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;
import com.manhnv.capstonebakerai.user.service.UserSummaryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.exception.RecipeCatalogErrorCode;
import com.manhnv.capstonebakerai.recipe.exception.RecipeErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository repository;
    private final UserRepository userRepository;
    private final RecipeCategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasService ingredientAliasService;
    private final ToolRepository toolRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeToolRepository recipeToolRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeImageRepository recipeImageRepository;
    private final RecipeStepImageRepository recipeStepImageRepository;
    private final RecipeMediaHelper mediaHelper;
    private final RecipeStepResourceHelper stepResourceHelper;
    private final RecipeStepDurationHelper durationHelper;
    private final UserSummaryService userSummaryService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public RecipeResponse getById(UUID id) {
        Recipe entity = repository.findById(id)
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_NOT_FOUND));
        ensureCanView(entity);
        repository.incrementViewCount(id);
        return enrich(RecipeMapper.toDTO(entity), entity);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeResponse getDetail(UUID id) {
        Recipe entity = repository.findById(id)
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_NOT_FOUND));
        return enrich(RecipeMapper.toDTO(entity), entity);
    }

    private static final Set<String> INTERNAL_ROLES =
            Set.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_STAFF", "ROLE_BAKER_STAFF");

    private static boolean isOwner(Recipe recipe) {
        return recipe.getUser() != null
                && recipe.getUser().getUserId().equals(SecurityUtils.getCurrentUserId());
    }

    private static boolean isInternalStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> INTERNAL_ROLES.contains(a.getAuthority()));
    }

    private static void ensureCanView(Recipe recipe) {
        if (recipe.getRecipeStatus() == RecipeStatus.PERSONAL && !isOwner(recipe)) {
            throw new AppException(RecipeErrorCode.RECIPE_NOT_FOUND);
        }
    }

    private static void ensureCanModify(Recipe recipe) {
        if (isOwner(recipe)) {
            return;
        }
        if (recipe.getRecipeStatus() == RecipeStatus.PERSONAL || !isInternalStaff()) {
            throw new AppException(RecipeErrorCode.RECIPE_ACCESS_DENIED);
        }
    }

    private RecipeResponse enrich(RecipeResponse response, Recipe entity) {
        UUID userId = entity.getUser() != null ? entity.getUser().getUserId() : null;
        response.setUser(userSummaryService.getSummary(userId));
        return response;
    }

    @Override
    @Transactional
    public RecipeResponse create(RecipeRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(RecipeErrorCode.USER_NOT_FOUND));

        RecipeCategory category = request.getCategoryId() == null ? null
                : categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(RecipeCatalogErrorCode.CATEGORY_NOT_FOUND));

        mediaHelper.validateContinuousStepNumbers(nullToEmpty(request.getSteps()).stream()
                .map(RecipeStepEmbeddedRequest::getStepNumber)
                .toList());
        durationHelper.validateEmbeddedSteps(request.getPrepTimeMinutes(), request.getCookTimeMinutes(), request.getSteps());
        stepResourceHelper.validateEmbeddedRecipe(request.getIngredients(), request.getTools(), request.getSteps());

        List<UUID> requestedMediaIdList = collectRequestedMediaIds(request);
        mediaHelper.validateUniqueMediaIds(requestedMediaIdList);
        Set<UUID> requestedMediaIds = new HashSet<>(requestedMediaIdList);
        Map<UUID, Media> mediaById = mediaHelper.loadMediaMap(requestedMediaIds);
        Set<UUID> noCurrentlyAttachedMedia = Set.of();

        Recipe recipe = RecipeMapper.toEntity(request);
        if (request.getVideoId() != null) {
            recipe.setMedia(mediaHelper.resolveMedia(request.getVideoId(), MediaType.VIDEO, mediaById, noCurrentlyAttachedMedia));
        }
        recipe.setUser(user);
        recipe.setRecipeCategory(category);
        if (recipe.getRecipeStatus() == RecipeStatus.PUBLISHED) {
            recipe.setPublishedAt(LocalDateTime.now());
        }

        Recipe savedRecipe = repository.save(recipe);
        UUID recipeId = savedRecipe.getId();

        if (request.getImageIds() != null) {
            saveRecipeImages(savedRecipe, request.getImageIds(), mediaById, noCurrentlyAttachedMedia);
        }

        Map<UUID, RecipeTool> recipeToolsByToolId = byToolId(saveRecipeTools(savedRecipe, nullToEmpty(request.getTools())));
        Map<UUID, RecipeIngredient> recipeIngredientsByIngredientId =
                byIngredientId(saveRecipeIngredients(savedRecipe, nullToEmpty(request.getIngredients())));

        if (request.getSteps() != null) {
            List<RecipeStep> steps = request.getSteps().stream().map(stepRequest -> {
                RecipeStep step = new RecipeStep();
                step.setStepNumber(stepRequest.getStepNumber());
                step.setInstruction(stepRequest.getInstruction());
                step.setDurationMinutes(stepRequest.getDurationMinutes());
                step.setNotes(stepRequest.getNotes());
                step.setRecipeStepType(stepRequest.getRecipeStepType());
                step.setRecipe(savedRecipe);
                return step;
            }).collect(Collectors.toList());
            List<RecipeStep> savedSteps = recipeStepRepository.saveAll(steps);
            for (int i = 0; i < savedSteps.size(); i++) {
                RecipeStepEmbeddedRequest stepRequest = request.getSteps().get(i);
                stepResourceHelper.replaceEmbeddedResources(savedSteps.get(i), stepRequest.getIngredients(),
                        stepRequest.getTools(), recipeIngredientsByIngredientId, recipeToolsByToolId);
                List<UUID> imageIds = stepRequest.getImageIds();
                if (imageIds != null) mediaHelper.saveStepImages(savedSteps.get(i), imageIds, mediaById, noCurrentlyAttachedMedia);
            }
        }

        entityManager.flush();
        entityManager.clear();

        Recipe detail = repository.findById(recipeId)
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_NOT_FOUND));


        return enrich(RecipeMapper.toDTO(detail), detail);
    }

    @Override
    @Transactional
    public RecipeResponse update(UUID recipeId, RecipeRequest request) {
        Recipe existing = repository.findById(recipeId)
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_NOT_FOUND));
        ensureCanModify(existing);
        if (request.getRecipeStatus() == RecipeStatus.PERSONAL
                && existing.getRecipeStatus() != RecipeStatus.PERSONAL
                && !isOwner(existing)) {
            throw new AppException(RecipeErrorCode.PERSONAL_STATUS_LOCKED);
        }

        List<RecipeStepEmbeddedRequest> steps = nullToEmpty(request.getSteps());
        List<UUID> imageIds = nullToEmpty(request.getImageIds());
        List<com.manhnv.capstonebakerai.recipe.dto.request.ToolEmbeddedRequest> tools = nullToEmpty(request.getTools());
        List<com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest> ingredients = nullToEmpty(request.getIngredients());

        mediaHelper.validateContinuousStepNumbers(steps.stream()
                .map(RecipeStepEmbeddedRequest::getStepNumber)
                .toList());
        durationHelper.validateEmbeddedSteps(request.getPrepTimeMinutes(), request.getCookTimeMinutes(), steps);
        stepResourceHelper.validateEmbeddedRecipe(ingredients, tools, steps);

        List<RecipeStep> existingSteps = recipeStepRepository.findByRecipe_IdOrderByStepNumberAsc(recipeId);

        Set<UUID> currentlyAttachedMediaIds = new HashSet<>();
        if (existing.getMedia() != null) currentlyAttachedMediaIds.add(existing.getMedia().getId());
        currentlyAttachedMediaIds.addAll(recipeImageRepository.findMediaIdsByRecipeId(recipeId));
        currentlyAttachedMediaIds.addAll(recipeStepImageRepository.findMediaIdsByRecipeId(recipeId));

        List<UUID> requestedMediaIdList = collectRequestedMediaIds(request.getVideoId(), imageIds, steps);
        mediaHelper.validateUniqueMediaIds(requestedMediaIdList);
        Set<UUID> allMediaIds = new HashSet<>(currentlyAttachedMediaIds);
        allMediaIds.addAll(requestedMediaIdList);
        Map<UUID, Media> mediaById = mediaHelper.loadMediaMap(allMediaIds);

        Set<UUID> mediaIdsInUseAfterUpdate = new HashSet<>(requestedMediaIdList);

        RecipeCategory category = request.getCategoryId() == null ? null
                : categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(RecipeCatalogErrorCode.CATEGORY_NOT_FOUND));
        existing.setRecipeCategory(category);

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setMedia(request.getVideoId() == null ? null
                : mediaHelper.resolveMedia(request.getVideoId(), MediaType.VIDEO, mediaById, currentlyAttachedMediaIds));
        existing.setDifficulty(request.getDifficulty());
        existing.setPrepTimeMinutes(request.getPrepTimeMinutes());
        existing.setCookTimeMinutes(request.getCookTimeMinutes());
        existing.setServings(request.getServings());
        if (request.getRecipeStatus() == RecipeStatus.PUBLISHED && existing.getRecipeStatus() != RecipeStatus.PUBLISHED) {
            existing.setPublishedAt(LocalDateTime.now());
        }
        existing.setRecipeStatus(request.getRecipeStatus());

        repository.save(existing);

        recipeImageRepository.deleteByRecipeId(recipeId);
        recipeImageRepository.flush();
        saveRecipeImages(existing, imageIds, mediaById, currentlyAttachedMediaIds);

        if (!existingSteps.isEmpty()) {
            recipeStepImageRepository.deleteByRecipeStepIds(existingSteps.stream().map(RecipeStep::getId).toList());
            recipeStepImageRepository.flush();
            recipeStepRepository.deleteAll(existingSteps);
            recipeStepRepository.flush();
        }

        recipeToolRepository.deleteByRecipe_Id(recipeId);
        recipeToolRepository.flush();
        Map<UUID, RecipeTool> recipeToolsByToolId = byToolId(saveRecipeTools(existing, tools));

        recipeIngredientRepository.deleteByRecipe_Id(recipeId);
        recipeIngredientRepository.flush();
        Map<UUID, RecipeIngredient> recipeIngredientsByIngredientId =
                byIngredientId(saveRecipeIngredients(existing, ingredients));

        if (!steps.isEmpty()) {
            List<RecipeStep> newSteps = steps.stream().map(stepRequest -> {
                RecipeStep newStep = new RecipeStep();
                newStep.setStepNumber(stepRequest.getStepNumber());
                newStep.setInstruction(stepRequest.getInstruction());
                newStep.setDurationMinutes(stepRequest.getDurationMinutes());
                newStep.setNotes(stepRequest.getNotes());
                newStep.setRecipeStepType(stepRequest.getRecipeStepType());
                newStep.setRecipe(existing);
                return newStep;
            }).toList();

            List<RecipeStep> savedSteps = recipeStepRepository.saveAll(newSteps);
            for (int i = 0; i < savedSteps.size(); i++) {
                RecipeStepEmbeddedRequest stepRequest = steps.get(i);
                stepResourceHelper.replaceEmbeddedResources(savedSteps.get(i), stepRequest.getIngredients(),
                        stepRequest.getTools(), recipeIngredientsByIngredientId, recipeToolsByToolId);
                List<UUID> stepImageIds = nullToEmpty(stepRequest.getImageIds());
                mediaHelper.saveStepImages(savedSteps.get(i), stepImageIds, mediaById, currentlyAttachedMediaIds);
            }
        }

        List<Media> detached = new ArrayList<>();
        List<UUID> detachedMediaIds = new ArrayList<>();
        for (UUID oldMediaId : currentlyAttachedMediaIds) {
            if (!mediaIdsInUseAfterUpdate.contains(oldMediaId)) {
                Media m = mediaById.get(oldMediaId);
                if (m != null) {
                    detached.add(m);
                    detachedMediaIds.add(oldMediaId);
                }
            }
        }
        mediaHelper.detachMedia(detached);

        entityManager.flush();
        entityManager.clear();

        mediaHelper.deleteMediaAfterCommit(detachedMediaIds);

        Recipe detail = repository.findById(recipeId)
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_NOT_FOUND));


        return enrich(RecipeMapper.toDTO(detail), detail);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Recipe recipe = repository.findById(id)
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_NOT_FOUND));
        ensureCanModify(recipe);

        if (recipe.getRecipeStatus() == RecipeStatus.HIDDEN) {
            throw new AppException(RecipeErrorCode.RECIPE_ALREADY_HIDDEN);
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

        List<Recipe> recipes = page.getContent();
        Map<UUID, UserSummaryResponse> summariesByUserId = userSummaryService.getSummaries(recipes.stream()
                .map(r -> r.getUser() != null ? r.getUser().getUserId() : null)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet()));

        List<RecipeResponse> content = recipes.stream().map(r -> {
            RecipeResponse dto = RecipeMapper.toDTO(r);
            dto.setUser(r.getUser() != null ? summariesByUserId.get(r.getUser().getUserId()) : null);
            return dto;
        }).toList();

        return PageResponse.<RecipeResponse>builder()
                .content(content)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    private List<RecipeTool> saveRecipeTools(Recipe recipe, List<ToolEmbeddedRequest> requests) {
        return recipeToolRepository.saveAll(requests.stream().map(r -> {
            Tool tool = toolRepository.findById(r.getToolId()).orElseThrow(() -> new AppException(RecipeCatalogErrorCode.TOOL_NOT_FOUND));
            return RecipeTool.builder()
                    .recipe(recipe)
                    .tool(tool)
                    .rawName(tool.getName())
                    .quantity(r.getQuantity())
                    .note(r.getNote())
                    .build();
        }).toList());
    }

    private List<RecipeIngredient> saveRecipeIngredients(Recipe recipe, List<IngredientEmbeddedRequest> requests) {
        return recipeIngredientRepository.saveAll(requests.stream().map(r -> {
            Ingredient ingredient = ingredientRepository.findById(r.getIngredientId())
                    .orElseThrow(() -> new AppException(com.manhnv.capstonebakerai.recipe.ingredientalias.exception.IngredientAliasErrorCode.INGREDIENT_NOT_FOUND));
            if (r.getAliases() != null && !r.getAliases().isEmpty()) {
                ingredientAliasService.addAll(ingredient, r.getAliases());
            }
            return RecipeIngredient.builder()
                    .recipe(recipe)
                    .ingredient(ingredient)
                    .rawName(ingredient.getName())
                    .quantity(r.getQuantity())
                    .unit(requireUnit(r))
                    .note(r.getNote())
                    .build();
        }).toList());
    }

    private static Map<UUID, RecipeTool> byToolId(List<RecipeTool> links) {
        Map<UUID, RecipeTool> map = new HashMap<>();
        for (RecipeTool link : nullToEmpty(links)) {
            map.put(link.getTool().getId(), link);
        }
        return map;
    }

    private static Map<UUID, RecipeIngredient> byIngredientId(List<RecipeIngredient> links) {
        Map<UUID, RecipeIngredient> map = new HashMap<>();
        for (RecipeIngredient link : nullToEmpty(links)) {
            map.put(link.getIngredient().getId(), link);
        }
        return map;
    }

    private static String requireUnit(com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest request) {
        if (request.getUnit() == null || request.getUnit().isBlank()) {
            throw new com.manhnv.capstonebakerai.common.exception.AppException(
                    com.manhnv.capstonebakerai.common.exception.CommonErrorCode.INVALID_REQUEST,
                    "unit is required for every recipe ingredient");
        }
        return request.getUnit().trim();
    }

    private void saveRecipeImages(Recipe recipe, List<UUID> mediaIds, Map<UUID, Media> mediaById,
                                  Set<UUID> currentlyAttachedMediaIds) {
        mediaHelper.validateUniqueMediaIds(mediaIds);
        List<RecipeImage> links = new ArrayList<>();
        for (int index = 0; index < mediaIds.size(); index++) {
            links.add(RecipeImage.builder()
                    .recipe(recipe)
                    .media(mediaHelper.resolveMedia(mediaIds.get(index), MediaType.IMAGE, mediaById, currentlyAttachedMediaIds))
                    .sortOrder(index + 1)
                    .build());
        }
        recipeImageRepository.saveAll(links);
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

}
