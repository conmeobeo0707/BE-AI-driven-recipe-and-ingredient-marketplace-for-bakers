package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeVideoRender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeVideoRenderRepository extends JpaRepository<RecipeVideoRender, UUID> {
    Optional<RecipeVideoRender> findByRenderId(String renderId);

    boolean existsByRecipeIdAndStatus(UUID recipeId, String status);

    Optional<RecipeVideoRender> findByRecipeIdAndStatus(UUID recipeId, String status);

    List<RecipeVideoRender> findAllByStatus(String status);
}
