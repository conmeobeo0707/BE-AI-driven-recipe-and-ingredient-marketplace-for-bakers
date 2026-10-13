package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecipeRatingRepository extends JpaRepository<RecipeRating, UUID> {
    List<RecipeRating> findByRecipe_Id(UUID recipeId);
    boolean existsByRecipe_IdAndCustomer_Id(UUID recipeId, UUID customerId);

    Page<RecipeRating> findAll(Pageable pageable);
    Page<RecipeRating> findByScore(Integer score, Pageable pageable);

    Page<RecipeRating> findByCustomer_Id(UUID customerId, Pageable pageable);
    Page<RecipeRating> findByCustomer_IdAndScore(UUID customerId, Integer score, Pageable pageable);

    Page<RecipeRating> findByRecipe_Id(UUID recipeId, Pageable pageable);
    Page<RecipeRating> findByRecipe_IdAndScore(UUID recipeId, Integer score, Pageable pageable);
}