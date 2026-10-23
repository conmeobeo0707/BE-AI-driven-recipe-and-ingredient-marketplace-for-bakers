package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID>, JpaSpecificationExecutor<Recipe> {

    List<Recipe> findByIdInAndRecipeStatus(Collection<UUID> ids,
                                           com.manhnv.capstonebakerai.recipe.enums.RecipeStatus recipeStatus);

    List<Recipe> findByRecipeStatusOrderByCreatedAtDesc(
            com.manhnv.capstonebakerai.recipe.enums.RecipeStatus recipeStatus);
}
