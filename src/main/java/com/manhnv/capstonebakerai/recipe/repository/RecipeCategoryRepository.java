package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Repository
public interface RecipeCategoryRepository extends JpaRepository<RecipeCategory, UUID> {
    Page<RecipeCategory> findByNameContainingIgnoreCase(String name, Pageable pageable);
}