package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.videoextraction.client.RawExtractedRecipe;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class ExtractedRecipeSanitizer {

    private static final int MAX_TITLE = 255;
    private static final int MAX_DESCRIPTION = 255;
    private static final int MAX_NAME = 150;
    private static final int MAX_UNIT = 50;
    private static final int MAX_NOTE = 500;
    private static final int MAX_ITEMS = 100;

    public ExtractedRecipe sanitize(RawExtractedRecipe raw) {
        if (raw == null || !Boolean.TRUE.equals(raw.isRecipe())) {
            return ExtractedRecipe.notRecipe();
        }

        Map<String, String> toolKeys = new HashMap<>();
        List<ExtractedRecipe.Tool> tools = new ArrayList<>();
        Set<String> usedToolKeys = new HashSet<>();
        for (RawExtractedRecipe.Tool tool : nullToEmpty(raw.tools())) {
            String name = tool == null ? null : clean(tool.name(), MAX_NAME);
            if (name == null || tools.size() >= MAX_ITEMS) {
                continue;
            }
            String key = assignKey(tool.key(), "t", usedToolKeys, toolKeys);
            tools.add(new ExtractedRecipe.Tool(key, name, positiveOrNull(tool.quantity()), clean(tool.note(), MAX_NOTE)));
        }

        Map<String, String> ingredientKeys = new HashMap<>();
        List<ExtractedRecipe.Ingredient> ingredients = new ArrayList<>();
        Set<String> usedIngredientKeys = new HashSet<>();
        for (RawExtractedRecipe.Ingredient ingredient : nullToEmpty(raw.ingredients())) {
            String name = ingredient == null ? null : clean(ingredient.name(), MAX_NAME);
            if (name == null || ingredients.size() >= MAX_ITEMS) {
                continue;
            }
            String key = assignKey(ingredient.key(), "i", usedIngredientKeys, ingredientKeys);
            ingredients.add(new ExtractedRecipe.Ingredient(key, name, clean(ingredient.unit(), MAX_UNIT),
                    positiveOrNull(ingredient.quantity()), clean(ingredient.note(), MAX_NOTE)));
        }

        List<RawExtractedRecipe.Step> rawSteps = nullToEmpty(raw.steps()).stream()
                .filter(step -> step != null && clean(step.instruction(), Integer.MAX_VALUE) != null)
                .sorted(Comparator.comparing(RawExtractedRecipe.Step::stepNumber,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_ITEMS)
                .toList();

        List<ExtractedRecipe.Step> steps = new ArrayList<>();
        for (RawExtractedRecipe.Step step : rawSteps) {
            steps.add(new ExtractedRecipe.Step(
                    steps.size() + 1,
                    clean(step.instruction(), Integer.MAX_VALUE),
                    nonNegativeOrNull(step.durationMinutes()),
                    clean(step.notes(), Integer.MAX_VALUE),
                    parseEnum(RecipeStepType.class, step.recipeStepType(), RecipeStepType.COOKING),
                    refs(step.tools(), toolKeys),
                    refs(step.ingredients(), ingredientKeys)));
        }

        Integer prepTime = steps.isEmpty() ? nonNegativeOrNull(raw.prepTimeMinutes())
                : sumDurations(steps, RecipeStepType.PREPARATION);
        Integer cookTime = steps.isEmpty() ? nonNegativeOrNull(raw.cookTimeMinutes())
                : sumDurations(steps, RecipeStepType.COOKING);

        return new ExtractedRecipe(
                true,
                clean(raw.categoryName(), MAX_TITLE),
                clean(raw.title(), MAX_TITLE),
                clean(raw.description(), MAX_DESCRIPTION),
                parseEnum(DifficultyEnum.class, raw.difficulty(), null),
                prepTime,
                cookTime,
                raw.servings() != null && raw.servings() >= 1 ? raw.servings() : null,
                List.copyOf(tools),
                coverStepQuantities(ingredients, steps),
                List.copyOf(steps));
    }

    private static List<ExtractedRecipe.Ingredient> coverStepQuantities(List<ExtractedRecipe.Ingredient> ingredients,
                                                                        List<ExtractedRecipe.Step> steps) {
        Map<String, BigDecimal> usedInSteps = new HashMap<>();
        for (ExtractedRecipe.Step step : steps) {
            for (ExtractedRecipe.ItemRef ref : step.ingredients()) {
                if (ref.quantity() != null) {
                    usedInSteps.merge(ref.key(), ref.quantity(), BigDecimal::add);
                }
            }
        }
        return ingredients.stream()
                .map(ingredient -> {
                    BigDecimal used = usedInSteps.get(ingredient.key());
                    if (used == null || (ingredient.quantity() != null && ingredient.quantity().compareTo(used) >= 0)) {
                        return ingredient;
                    }
                    return new ExtractedRecipe.Ingredient(ingredient.key(), ingredient.name(), ingredient.unit(),
                            used, ingredient.note());
                })
                .toList();
    }

    private static String assignKey(String rawKey, String prefix, Set<String> usedKeys, Map<String, String> keyMap) {
        String cleaned = clean(rawKey, 20);
        String key = cleaned;
        if (key == null || usedKeys.contains(key)) {
            int index = usedKeys.size() + 1;
            while (usedKeys.contains(prefix + index)) {
                index++;
            }
            key = prefix + index;
        }
        usedKeys.add(key);
        if (cleaned != null) {
            keyMap.putIfAbsent(cleaned, key);
        }
        return key;
    }

    private static List<ExtractedRecipe.ItemRef> refs(List<RawExtractedRecipe.ItemRef> rawRefs,
                                                      Map<String, String> keyMap) {
        Map<String, ExtractedRecipe.ItemRef> refs = new LinkedHashMap<>();
        for (RawExtractedRecipe.ItemRef ref : nullToEmpty(rawRefs)) {
            String key = ref == null ? null : keyMap.get(clean(ref.key(), 20));
            if (key != null) {
                refs.putIfAbsent(key, new ExtractedRecipe.ItemRef(key, positiveOrNull(ref.quantity())));
            }
        }
        return List.copyOf(refs.values());
    }

    private static Integer sumDurations(List<ExtractedRecipe.Step> steps, RecipeStepType type) {
        List<Integer> durations = steps.stream()
                .filter(step -> step.recipeStepType() == type)
                .map(ExtractedRecipe.Step::durationMinutes)
                .filter(Objects::nonNull)
                .toList();
        return durations.isEmpty() ? null : durations.stream().mapToInt(Integer::intValue).sum();
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.signum() > 0 ? value : null;
    }

    private static Integer nonNegativeOrNull(Integer value) {
        return value != null && value >= 0 ? value : null;
    }

    private static String clean(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        int end = maxLength;
        if (Character.isHighSurrogate(trimmed.charAt(end - 1))) {
            end--;
        }
        return trimmed.substring(0, end).strip();
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
