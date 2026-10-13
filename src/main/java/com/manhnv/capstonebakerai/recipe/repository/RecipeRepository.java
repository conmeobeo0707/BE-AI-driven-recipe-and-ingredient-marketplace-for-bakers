package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RecipeRepository extends JpaRepository<Recipe, UUID>, JpaSpecificationExecutor<Recipe> {
}