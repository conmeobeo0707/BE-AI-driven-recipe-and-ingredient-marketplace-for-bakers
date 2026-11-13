package com.manhnv.capstonebakerai.recipe.repository;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, UUID> {
 void deleteByRecipe_Id(UUID recipeId);
 java.util.List<RecipeIngredient> findByRecipe_Id(UUID recipeId);
 boolean existsByRecipe_IdAndIngredient_Id(UUID recipeId, UUID ingredientId);
}
