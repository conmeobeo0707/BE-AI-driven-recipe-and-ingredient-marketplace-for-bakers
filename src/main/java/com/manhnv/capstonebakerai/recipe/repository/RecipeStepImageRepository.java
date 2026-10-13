package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeStepImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecipeStepImageRepository extends JpaRepository<RecipeStepImage, UUID> {

    @Query("SELECT image FROM RecipeStepImage image JOIN FETCH image.media " +
            "WHERE image.recipeStep.id = :recipeStepId ORDER BY image.sortOrder")
    List<RecipeStepImage> findByRecipeStepIdWithMedia(@Param("recipeStepId") UUID recipeStepId);

    @Query("SELECT image FROM RecipeStepImage image JOIN FETCH image.media " +
            "WHERE image.recipeStep.recipe.id = :recipeId " +
            "ORDER BY image.recipeStep.stepNumber, image.sortOrder")
    List<RecipeStepImage> findByRecipeIdWithMedia(@Param("recipeId") UUID recipeId);

    @Modifying
    @Query("DELETE FROM RecipeStepImage image WHERE image.recipeStep.id IN :recipeStepIds")
    void deleteByRecipeStepIds(@Param("recipeStepIds") List<UUID> recipeStepIds);

    // Chỉ lấy id media, không nạp entity vào session
    @Query("SELECT image.media.id FROM RecipeStepImage image " +
            "WHERE image.recipeStep.recipe.id = :recipeId")
    List<UUID> findMediaIdsByRecipeId(@Param("recipeId") UUID recipeId);
}
