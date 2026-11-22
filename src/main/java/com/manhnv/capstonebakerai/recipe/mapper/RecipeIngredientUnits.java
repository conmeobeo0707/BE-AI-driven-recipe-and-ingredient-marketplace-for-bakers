package com.manhnv.capstonebakerai.recipe.mapper;

import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;

import java.util.Objects;
import java.util.UUID;

// A step's ingredient has no unit of its own: it uses the unit the recipe gives that ingredient.
public final class RecipeIngredientUnits {

    private RecipeIngredientUnits() {
    }

    public static String unitOf(Recipe recipe, UUID ingredientId) {
        if (recipe == null || recipe.getIngredients() == null || ingredientId == null) {
            return null;
        }
        return recipe.getIngredients().stream()
                .filter(link -> link.getIngredient() != null && ingredientId.equals(link.getIngredient().getId()))
                .map(RecipeIngredient::getUnit)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
