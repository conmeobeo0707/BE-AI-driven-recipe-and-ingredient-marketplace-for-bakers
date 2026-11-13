package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    Page<Ingredient> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("select count(ri) > 0 from RecipeIngredient ri "
            + "where ri.ingredient.catalogIngredient.id = :catalogId and ri.recipe.recipeStatus = :status")
    boolean existsByCatalogIdUsedInRecipeStatus(@Param("catalogId") UUID catalogId, @Param("status") RecipeStatus status);

    @Query("select i.name as name, count(i) as rowCount from Ingredient i where i.catalogIngredient is null group by i.name")
    List<UnlinkedIngredientNameCount> countUnlinkedByName();

    @Modifying(flushAutomatically = true)
    @Query("update Ingredient i set i.catalogIngredient = :catalog where i.catalogIngredient is null and i.name in :names")
    int linkUnlinkedByNames(@Param("catalog") IngredientCatalog catalog, @Param("names") Collection<String> names);
}
