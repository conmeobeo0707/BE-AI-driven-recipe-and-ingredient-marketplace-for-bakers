package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.exception.CommonErrorCode;
import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import com.manhnv.capstonebakerai.media.repository.MediaRepository;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import com.manhnv.capstonebakerai.videoextraction.dto.RecipeVideoExtractionResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExtractedRecipePersister {

    private final ExtractedRecipeMatcher matcher;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeCategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final ToolRepository toolRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeToolRepository recipeToolRepository;
    private final RecipeStepRepository recipeStepRepository;
    private final RecipeStepIngredientRepository stepIngredientRepository;
    private final RecipeStepToolRepository stepToolRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public UUID save(UUID mediaId, UUID userId, ExtractedRecipe extracted) {
        RecipeVideoExtractionResponse result = matcher.match(mediaId, extracted, true);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(CommonErrorCode.UNAUTHENTICATED));

        Recipe recipe = Recipe.builder()
                .user(user)
                .recipeCategory(result.category() == null || result.category().categoryId() == null ? null
                        : categoryRepository.getReferenceById(result.category().categoryId()))
                .title(result.title())
                .description(result.description())
                .difficulty(result.difficulty() == null ? DifficultyEnum.MEDIUM : result.difficulty())
                .prepTimeMinutes(result.prepTimeMinutes())
                .cookTimeMinutes(result.cookTimeMinutes())
                .servings(result.servings())
                .recipeStatus(RecipeStatus.PERSONAL)
                .build();
        mediaRepository.findById(mediaId)
                .filter(media -> media.getStatus() == MediaStatus.TEMP)
                .ifPresent(media -> {
                    media.setStatus(MediaStatus.ATTACHED);
                    media.setTempSince(null);
                    recipe.setMedia(media);
                });
        Recipe saved = recipeRepository.save(recipe);

        Map<String, RecipeIngredient> ingredientsByKey = new LinkedHashMap<>();
        for (RecipeVideoExtractionResponse.Ingredient item : result.ingredients()) {
            ingredientsByKey.put(item.key(), RecipeIngredient.builder()
                    .recipe(saved)
                    .ingredient(item.matched() ? ingredientRepository.getReferenceById(item.ingredientId()) : null)
                    .rawName(item.matched() ? item.matchedName() : item.rawName())
                    .quantity(item.quantity())
                    .unit(item.unit())
                    .note(item.note())
                    .build());
        }
        recipeIngredientRepository.saveAll(ingredientsByKey.values());

        Map<String, RecipeTool> toolsByKey = new LinkedHashMap<>();
        for (RecipeVideoExtractionResponse.Tool item : result.tools()) {
            toolsByKey.put(item.key(), RecipeTool.builder()
                    .recipe(saved)
                    .tool(item.matched() ? toolRepository.getReferenceById(item.toolId()) : null)
                    .rawName(item.matched() ? item.matchedName() : item.rawName())
                    .quantity(item.quantity())
                    .note(item.note())
                    .build());
        }
        recipeToolRepository.saveAll(toolsByKey.values());

        List<RecipeStepIngredient> stepIngredients = new ArrayList<>();
        List<RecipeStepTool> stepTools = new ArrayList<>();
        for (RecipeVideoExtractionResponse.Step item : result.steps()) {
            RecipeStep step = recipeStepRepository.save(RecipeStep.builder()
                    .recipe(saved)
                    .stepNumber(item.stepNumber())
                    .instruction(item.instruction())
                    .durationMinutes(item.durationMinutes())
                    .notes(item.notes())
                    .recipeStepType(item.recipeStepType())
                    .build());
            for (RecipeVideoExtractionResponse.ItemRef ref : item.ingredients()) {
                RecipeIngredient link = ingredientsByKey.get(ref.key());
                if (link != null) {
                    stepIngredients.add(RecipeStepIngredient.builder()
                            .recipeStep(step)
                            .recipeIngredient(link)
                            .quantity(ref.quantity() != null ? ref.quantity() : link.getQuantity())
                            .build());
                }
            }
            for (RecipeVideoExtractionResponse.ItemRef ref : item.tools()) {
                RecipeTool link = toolsByKey.get(ref.key());
                if (link != null) {
                    stepTools.add(RecipeStepTool.builder()
                            .recipeStep(step)
                            .recipeTool(link)
                            .quantity(ref.quantity() != null ? ref.quantity() : link.getQuantity())
                            .build());
                }
            }
        }
        stepIngredientRepository.saveAll(stepIngredients);
        stepToolRepository.saveAll(stepTools);

        entityManager.flush();
        entityManager.clear();
        return saved.getId();
    }
}
