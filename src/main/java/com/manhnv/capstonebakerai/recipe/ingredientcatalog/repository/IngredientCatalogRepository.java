package com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository;

import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredientCatalogRepository
        extends JpaRepository<IngredientCatalog, UUID> {

    Optional<IngredientCatalog> findByNormalizedNameAndActiveTrue(String normalizedName);

    Optional<IngredientCatalog> findByNormalizedName(String normalizedName);

    List<IngredientCatalog> findAllByActiveTrueOrderByCanonicalNameAsc();

    List<IngredientCatalog> findAllByOrderByCanonicalNameAsc();

    List<IngredientCatalog> findAllByIdInAndActiveTrue(Collection<UUID> ids);

    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(39, 1)", nativeQuery = true)
    Integer lockNameRegistry();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from IngredientCatalog c where c.id = :id")
    Optional<IngredientCatalog> findForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from IngredientCatalog c where c.id = :id and c.active = true")
    Optional<IngredientCatalog> findActiveForBinding(@Param("id") UUID id);
}
