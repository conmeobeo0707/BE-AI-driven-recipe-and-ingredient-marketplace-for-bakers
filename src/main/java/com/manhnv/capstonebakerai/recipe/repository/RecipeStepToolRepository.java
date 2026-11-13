package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeStepToolRepository extends JpaRepository<RecipeStepTool, UUID> {
    void deleteByRecipeStep_Id(UUID recipeStepId);

    List<RecipeStepTool> findByRecipeStep_Id(UUID recipeStepId);

    List<RecipeStepTool> findByRecipeStep_Recipe_Id(UUID recipeId);
}
