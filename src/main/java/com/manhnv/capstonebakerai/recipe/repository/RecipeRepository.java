package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.suggestion.repository.RecipeIngredientMatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("select r.title from Recipe r where r.user.id = :userId and r.title is not null")
    List<String> findTitlesByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true)
    @Query("update Recipe r set r.viewCount = r.viewCount + 1 where r.id = :id")
    int incrementViewCount(@Param("id") UUID id);

    @Modifying(flushAutomatically = true)
    @Query("update Recipe r set r.bookmarkCount = r.bookmarkCount + 1 where r.id = :id")
    int incrementBookmarkCount(@Param("id") UUID id);

    @Modifying(flushAutomatically = true)
    @Query("update Recipe r set r.bookmarkCount = r.bookmarkCount - 1 where r.id = :id and r.bookmarkCount > 0")
    int decrementBookmarkCount(@Param("id") UUID id);

    @Modifying(flushAutomatically = true)
    @Query("update Recipe r set r.ratingCount = r.ratingCount + 1 where r.id = :id")
    int incrementRatingCount(@Param("id") UUID id);

    @Modifying(flushAutomatically = true)
    @Query("update Recipe r set r.ratingCount = r.ratingCount - 1 where r.id = :id and r.ratingCount > 0")
    int decrementRatingCount(@Param("id") UUID id);

    @Query("""
    select r.id as recipeId,
           count(distinct ri.ingredient.id) as matchedCount
    from Recipe r
    join r.ingredients ri
    where r.recipeStatus = :status
      and ri.ingredient.id in :ingredientIds
    group by r.id
    """)
    List<RecipeIngredientMatch> findIngredientMatches(
            @Param("status") RecipeStatus status,
            @Param("ingredientIds") Collection<UUID> ingredientIds
    );

    @EntityGraph(attributePaths = {
            "ingredients",
            "ingredients.ingredient"
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
