package com.manhnv.capstonebakerai.recipe.ingredientalias.repository;

import com.manhnv.capstonebakerai.recipe.ingredientalias.entity.IngredientAlias;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, UUID> {

    @EntityGraph(attributePaths = "ingredient")
    Optional<IngredientAlias> findByNormalizedAlias(String normalizedAlias);

    @EntityGraph(attributePaths = "ingredient")
    List<IngredientAlias> findAllByNormalizedAliasIn(Collection<String> normalizedAliases);

    Optional<IngredientAlias> findByIdAndIngredient_Id(UUID id, UUID ingredientId);

    List<IngredientAlias> findAllByIngredient_IdOrderByAliasAsc(UUID ingredientId);

    @Override
    @EntityGraph(attributePaths = "ingredient")
    List<IngredientAlias> findAll();

    // Serializes every change to ingredient names and aliases (same lock id the catalog used).
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(39, 1)", nativeQuery = true)
    Integer lockNameRegistry();
}
