package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {
    List<Ingredient> findByRecipe_Id(UUID recipeId);

    Page<Ingredient> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Ingredient> findByRecipe_IdAndNameContainingIgnoreCase(UUID recipeId, String name, Pageable pageable);

    Page<Ingredient> findByRecipe_Id(UUID recipeId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Ingredient i WHERE i.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") UUID recipeId);

    List<Ingredient> findByCatalogIngredientIsNullOrderById(Pageable pageable);

    long countByCatalogIngredientIsNull();

    boolean existsByCatalogIngredient_IdAndRecipe_RecipeStatus(UUID catalogId, RecipeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Ingredient i where i.id = :id")
    Optional<Ingredient> findForBackfill(@Param("id") UUID id);
}
