package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RecipeBookmarkRepository extends JpaRepository<RecipeBookmark, UUID> {
    List<RecipeBookmark> findByRecipe_Id(UUID recipeId);
    boolean existsByRecipe_IdAndCustomer_Id(UUID recipeId, UUID customerId);
    Page<RecipeBookmark> findByCustomer_Id(UUID customerId, Pageable pageable);
    Page<RecipeBookmark> findByRecipe_Id(UUID recipeId, Pageable pageable);

    @Query("select b.recipe.id from RecipeBookmark b where b.customer.id = :customerId and b.recipe.id in :recipeIds")
    Set<UUID> findRecipeIdsByCustomer(@Param("customerId") UUID customerId,
                                      @Param("recipeIds") Collection<UUID> recipeIds);
}