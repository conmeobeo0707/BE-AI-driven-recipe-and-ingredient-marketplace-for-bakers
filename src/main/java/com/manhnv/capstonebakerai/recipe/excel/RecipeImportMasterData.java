package com.manhnv.capstonebakerai.recipe.excel;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.entity.Tool;
import com.manhnv.capstonebakerai.recipe.ingredientalias.entity.IngredientAlias;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientNormalizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class RecipeImportMasterData {

    private final Map<String, List<RecipeCategory>> categories;
    private final Map<String, List<Tool>> tools;
    private final Map<String, List<Ingredient>> ingredientsByName;
    private final Map<String, List<Ingredient>> ingredientsByAlias;
    private final IngredientNormalizer normalizer;

    private RecipeImportMasterData(Collection<RecipeCategory> categories, Collection<Ingredient> ingredients,
                                   Collection<IngredientAlias> aliases, Collection<Tool> tools,
                                   IngredientNormalizer normalizer) {
        this.normalizer = normalizer;
        this.categories = group(categories, category -> simpleKey(category.getName()));
        this.tools = group(tools, tool -> simpleKey(tool.getName()));
        this.ingredientsByName = group(ingredients, ingredient -> normalizer.normalize(ingredient.getName()));
        Map<String, List<Ingredient>> byAlias = new HashMap<>();
        for (IngredientAlias alias : aliases) {
            if (alias.getIngredient() != null && alias.getNormalizedAlias() != null) {
                List<Ingredient> owners = byAlias.computeIfAbsent(alias.getNormalizedAlias(), key -> new ArrayList<>());
                if (owners.stream().noneMatch(owner -> owner.getId().equals(alias.getIngredient().getId()))) {
                    owners.add(alias.getIngredient());
                }
            }
        }
        this.ingredientsByAlias = byAlias;
    }

    public static RecipeImportMasterData of(Collection<RecipeCategory> categories, Collection<Ingredient> ingredients,
                                            Collection<IngredientAlias> aliases, Collection<Tool> tools,
                                            IngredientNormalizer normalizer) {
        return new RecipeImportMasterData(categories, ingredients, aliases, tools, normalizer);
    }

    public Lookup<RecipeCategory> category(String name) {
        return Lookup.of(categories.get(simpleKey(name)));
    }

    public Lookup<Tool> tool(String name) {
        return Lookup.of(tools.get(simpleKey(name)));
    }

    public Lookup<Ingredient> ingredient(String name) {
        String key = normalizer.normalize(name);
        if (key.isEmpty()) {
            return Lookup.of(null);
        }
        List<Ingredient> byAlias = ingredientsByAlias.get(key);
        if (byAlias != null && !byAlias.isEmpty()) {
            return Lookup.of(byAlias);
        }
        return Lookup.of(ingredientsByName.get(key));
    }

    private static String simpleKey(String name) {
        return name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
    }

    private static <T> Map<String, List<T>> group(Collection<T> items, Function<T, String> keyOf) {
        Map<String, List<T>> result = new HashMap<>();
        for (T item : items) {
            String key = keyOf.apply(item);
            if (key != null && !key.isEmpty()) {
                result.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }
        }
        return result;
    }

    public record Lookup<T>(T value, boolean ambiguous) {

        static <T> Lookup<T> of(List<T> matches) {
            if (matches == null || matches.isEmpty()) {
                return new Lookup<>(null, false);
            }
            return matches.size() == 1 ? new Lookup<>(matches.get(0), false) : new Lookup<>(null, true);
        }

        public boolean found() {
            return value != null;
        }
    }
}
