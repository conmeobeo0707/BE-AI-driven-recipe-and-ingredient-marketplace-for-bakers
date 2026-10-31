package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.suggestion.repository.RecipeIngredientMatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID>, JpaSpecificationExecutor<Recipe> {

    List<Recipe> findByIdInAndRecipeStatus(Collection<UUID> ids,
                                           com.manhnv.capstonebakerai.recipe.enums.RecipeStatus recipeStatus);

    List<Recipe> findByRecipeStatusOrderByCreatedAtDesc(
            com.manhnv.capstonebakerai.recipe.enums.RecipeStatus recipeStatus);

    @Query("""
    select r.id as recipeId,
           count(distinct i.catalogIngredient.id) as matchedCount
    from Recipe r
    join r.ingredients i
    where r.recipeStatus = :status
      and i.catalogIngredient.id in :catalogIngredientIds
    group by r.id
    """)
    List<RecipeIngredientMatch> findIngredientMatches(
            @Param("status") RecipeStatus status,
            @Param("catalogIngredientIds") Collection<UUID> catalogIngredientIds
    );

    @EntityGraph(attributePaths = {
            "ingredients",
            "ingredients.catalogIngredient"
    })
    @Query("""
        select distinct r
        from Recipe r
        where r.id in :recipeIds
          and r.recipeStatus = :status
        """)
    List<Recipe> findSuggestionRecipesWithIngredients(
            @Param("recipeIds") Collection<UUID> recipeIds,
            @Param("status") RecipeStatus status
    );
}
