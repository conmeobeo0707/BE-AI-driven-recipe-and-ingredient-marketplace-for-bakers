package com.manhnv.capstonebakerai.recipe.repository;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RecipeToolRepository extends JpaRepository<RecipeTool, UUID> {
 void deleteByRecipe_Id(UUID recipeId);
 java.util.List<RecipeTool> findByRecipe_Id(UUID recipeId);
 boolean existsByRecipe_IdAndTool_Id(UUID recipeId, UUID toolId);
}
