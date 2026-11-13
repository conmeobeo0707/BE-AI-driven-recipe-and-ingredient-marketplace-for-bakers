package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeStepIngredientRepository extends JpaRepository<RecipeStepIngredient, UUID> {
    void deleteByRecipeStep_Id(UUID recipeStepId);

    List<RecipeStepIngredient> findByRecipeStep_Id(UUID recipeStepId);

    List<RecipeStepIngredient> findByRecipeStep_Recipe_Id(UUID recipeId);
}
