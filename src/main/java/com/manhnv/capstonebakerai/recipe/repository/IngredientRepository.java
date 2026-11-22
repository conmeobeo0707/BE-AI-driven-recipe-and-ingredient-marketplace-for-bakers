package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    Page<Ingredient> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Shared ingredients only: rows still holding the old recipe_id belong to one old recipe and are not picked from.
    @Query(value = "SELECT * FROM ingredients WHERE recipe_id IS NULL ORDER BY name, id", nativeQuery = true)
    List<Ingredient> findAllShared();

    @Query(value = "SELECT * FROM ingredients WHERE recipe_id IS NULL AND id IN (:ids)", nativeQuery = true)
    List<Ingredient> findAllSharedByIdIn(@Param("ids") Collection<UUID> ids);
}
