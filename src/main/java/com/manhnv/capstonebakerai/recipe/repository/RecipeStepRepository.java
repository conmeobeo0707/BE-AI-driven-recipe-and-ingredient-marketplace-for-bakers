package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecipeStepRepository extends JpaRepository<RecipeStep, UUID> {
    List<RecipeStep> findByRecipe_IdOrderByStepNumberAsc(UUID recipeId);

    Page<RecipeStep> findByInstructionContainingIgnoreCase(String instruction, Pageable pageable);

    Page<RecipeStep> findByRecipe_IdAndInstructionContainingIgnoreCase(UUID recipeId, String instruction, Pageable pageable);

    Page<RecipeStep> findByRecipe_Id(UUID recipeId, Pageable pageable);
}