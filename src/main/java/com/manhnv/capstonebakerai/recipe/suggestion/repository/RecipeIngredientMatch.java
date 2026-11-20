package com.manhnv.capstonebakerai.recipe.suggestion.repository;

import java.util.UUID;

public interface RecipeIngredientMatch {
    UUID getRecipeId();
    Long getMatchedCount();
}
