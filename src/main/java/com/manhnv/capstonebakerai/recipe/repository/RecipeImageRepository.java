package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecipeImageRepository extends JpaRepository<RecipeImage, UUID> {

    @Query("SELECT image FROM RecipeImage image JOIN FETCH image.media " +
            "WHERE image.recipe.id = :recipeId ORDER BY image.sortOrder")
    List<RecipeImage> findByRecipeIdWithMedia(@Param("recipeId") UUID recipeId);

    @Modifying
    @Query("DELETE FROM RecipeImage image WHERE image.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") UUID recipeId);

    @Query("SELECT image.media.id FROM RecipeImage image " +
            "WHERE image.recipe.id = :recipeId")
    List<UUID> findMediaIdsByRecipeId(@Param("recipeId") UUID recipeId);
}
