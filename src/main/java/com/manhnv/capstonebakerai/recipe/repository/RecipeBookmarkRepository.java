package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeBookmarkRepository extends JpaRepository<RecipeBookmark, UUID> {
    List<RecipeBookmark> findByRecipe_Id(UUID recipeId);
    boolean existsByRecipe_IdAndCustomer_Id(UUID recipeId, UUID customerId);
    Page<RecipeBookmark> findByCustomer_Id(UUID customerId, Pageable pageable);
    Page<RecipeBookmark> findByRecipe_Id(UUID recipeId, Pageable pageable);
}