package com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository;

import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientAlias;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface IngredientAliasRepository
        extends JpaRepository<IngredientAlias, UUID> {

    @Query("""
        select a.ingredient
        from IngredientAlias a
        where a.normalizedAlias = :normalizedAlias
          and a.ingredient.active = true
        """)
    Optional<IngredientCatalog> findActiveIngredientByNormalizedAlias(
            @Param("normalizedAlias") String normalizedAlias
    );

    Optional<IngredientAlias> findByNormalizedAlias(String normalizedAlias);

    Optional<IngredientAlias> findByIdAndIngredient_Id(UUID id, UUID ingredientId);

    List<IngredientAlias> findAllByIngredient_IdOrderByAliasAsc(UUID ingredientId);

    @EntityGraph(attributePaths = "ingredient")
    List<IngredientAlias> findAllByIngredient_ActiveTrue();
}
