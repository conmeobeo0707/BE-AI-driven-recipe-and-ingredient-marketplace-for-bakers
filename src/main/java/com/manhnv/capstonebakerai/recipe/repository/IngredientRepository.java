package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {
    List<Ingredient> findByRecipe_Id(UUID recipeId);

    Page<Ingredient> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Ingredient> findByRecipe_IdAndNameContainingIgnoreCase(UUID recipeId, String name, Pageable pageable);

    Page<Ingredient> findByRecipe_Id(UUID recipeId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Ingredient i WHERE i.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") UUID recipeId);
}