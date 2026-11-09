package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import org.springframework.data.repository.query.Param;

import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;

import java.util.Collection;
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


    boolean existsByCatalogIngredient_IdAndRecipe_RecipeStatus(UUID catalogId, RecipeStatus status);


    @Query("select i.name as name, count(i) as rowCount from Ingredient i where i.catalogIngredient is null group by i.name")
    List<UnlinkedIngredientNameCount> countUnlinkedByName();

    @Modifying(flushAutomatically = true)
    @Query("update Ingredient i set i.catalogIngredient = :catalog where i.catalogIngredient is null and i.name in :names")
    int linkUnlinkedByNames(@Param("catalog") IngredientCatalog catalog, @Param("names") Collection<String> names);
}
