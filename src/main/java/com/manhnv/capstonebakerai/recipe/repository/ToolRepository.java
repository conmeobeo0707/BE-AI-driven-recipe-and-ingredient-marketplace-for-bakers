package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Tool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ToolRepository extends JpaRepository<Tool, UUID> {
    List<Tool> findByRecipe_Id(UUID recipeId);

    Page<Tool> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Tool> findByRecipe_IdAndNameContainingIgnoreCase(UUID recipeId, String name, Pageable pageable);

    Page<Tool> findByRecipe_Id(UUID recipeId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Tool t WHERE t.recipe.id = :recipeId")
    void deleteByRecipeId(@Param("recipeId") UUID recipeId);
}