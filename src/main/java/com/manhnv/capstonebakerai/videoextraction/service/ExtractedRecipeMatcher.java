package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.entity.Tool;
import com.manhnv.capstonebakerai.recipe.ingredientalias.entity.IngredientAlias;
import com.manhnv.capstonebakerai.recipe.ingredientalias.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientNormalizer;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.videoextraction.dto.RecipeVideoExtractionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExtractedRecipeMatcher {

    public static final String OTHER_CATEGORY_NAME = "Khác";

    private static final int MAX_CATEGORY_HINTS = 100;

    private final RecipeCategoryRepository categoryRepository;
    private final ToolRepository toolRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;
    private final IngredientNormalizer normalizer;

    public List<String> categoryNameHints() {
        return names(categoryRepository.findAll().stream().map(RecipeCategory::getName).toList(), MAX_CATEGORY_HINTS);
    }

    public RecipeVideoExtractionResponse match(UUID mediaId, ExtractedRecipe recipe, boolean createOtherCategory) {
        if (!recipe.isRecipe()) {
            return new RecipeVideoExtractionResponse(mediaId, false, null, null, null, null, null, null,
                    null, List.of(), List.of(), List.of());
        }
        return new RecipeVideoExtractionResponse(
                mediaId,
                true,
                recipe.title(),
                recipe.description(),
                recipe.difficulty(),
                recipe.prepTimeMinutes(),
                recipe.cookTimeMinutes(),
                recipe.servings(),
                matchCategory(recipe.categoryName(), createOtherCategory),
                matchIngredients(recipe.ingredients()),
                matchTools(recipe.tools()),
                recipe.steps().stream().map(ExtractedRecipeMatcher::toStep).toList());
    }

    private RecipeVideoExtractionResponse.Category matchCategory(String rawName, boolean createOther) {
        Map<String, RecipeCategory> byName = byNormalizedName(categoryRepository.findAll(), RecipeCategory::getName);
        RecipeCategory matched = byName.get(normalizer.normalize(rawName));
        if (matched != null) {
            return new RecipeVideoExtractionResponse.Category(rawName, matched.getId(), true, matched.getName());
        }
        RecipeCategory other = byName.get(normalizer.normalize(OTHER_CATEGORY_NAME));
        if (other == null && createOther) {
            other = categoryRepository.save(RecipeCategory.builder().name(OTHER_CATEGORY_NAME).build());
        }
        return new RecipeVideoExtractionResponse.Category(rawName,
                other == null ? null : other.getId(), false, other == null ? null : other.getName());
    }

    private List<RecipeVideoExtractionResponse.Ingredient> matchIngredients(List<ExtractedRecipe.Ingredient> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        Set<String> normalizedNames = items.stream()
                .map(item -> normalizer.normalize(item.name()))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());
        Map<String, Ingredient> byAlias = new HashMap<>();
        for (IngredientAlias alias : aliasRepository.findAllByNormalizedAliasIn(normalizedNames)) {
            byAlias.putIfAbsent(alias.getNormalizedAlias(), alias.getIngredient());
        }
        Map<String, Ingredient> byName = byNormalizedName(ingredientRepository.findAllByOrderByNameAscIdAsc(), Ingredient::getName);

        Set<UUID> usedIds = new HashSet<>();
        return items.stream().map(item -> {
            String normalized = normalizer.normalize(item.name());
            Ingredient ingredient = byAlias.getOrDefault(normalized, byName.get(normalized));
            boolean matched = ingredient != null && usedIds.add(ingredient.getId());
            return new RecipeVideoExtractionResponse.Ingredient(item.key(), item.name(), item.quantity(), item.unit(),
                    item.note(), matched,
                    matched ? ingredient.getId() : null,
                    matched ? ingredient.getName() : null);
        }).toList();
    }

    private List<RecipeVideoExtractionResponse.Tool> matchTools(List<ExtractedRecipe.Tool> items) {
        if (items.isEmpty()) {
            return List.of();
        }
        Map<String, Tool> byName = byNormalizedName(toolRepository.findAll(), Tool::getName);
        Set<UUID> usedIds = new HashSet<>();
        return items.stream().map(item -> {
            Tool tool = byName.get(normalizer.normalize(item.name()));
            boolean matched = tool != null && usedIds.add(tool.getId());
            return new RecipeVideoExtractionResponse.Tool(item.key(), item.name(), item.quantity(), item.note(),
                    matched,
                    matched ? tool.getId() : null,
                    matched ? tool.getName() : null);
        }).toList();
    }

    private static RecipeVideoExtractionResponse.Step toStep(ExtractedRecipe.Step step) {
        return new RecipeVideoExtractionResponse.Step(
                step.stepNumber(),
                step.instruction(),
                step.durationMinutes(),
                step.notes(),
                step.recipeStepType(),
                step.tools().stream().map(ExtractedRecipeMatcher::toRef).toList(),
                step.ingredients().stream().map(ExtractedRecipeMatcher::toRef).toList());
    }

    private static RecipeVideoExtractionResponse.ItemRef toRef(ExtractedRecipe.ItemRef ref) {
        return new RecipeVideoExtractionResponse.ItemRef(ref.key(), ref.quantity());
    }

    private <T> Map<String, T> byNormalizedName(List<T> entities, Function<T, String> name) {
        Map<String, T> map = new HashMap<>();
        for (T entity : entities) {
            String normalized = normalizer.normalize(name.apply(entity));
            if (!normalized.isEmpty()) {
                map.putIfAbsent(normalized, entity);
            }
        }
        return map;
    }

    private static List<String> names(List<String> names, int limit) {
        return names.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .limit(limit)
                .toList();
    }
}
