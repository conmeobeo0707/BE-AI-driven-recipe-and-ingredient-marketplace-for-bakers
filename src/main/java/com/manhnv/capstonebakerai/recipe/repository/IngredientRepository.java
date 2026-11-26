package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IngredientRepository extends JpaRepository<Ingredient, UUID> {

    Page<Ingredient> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Ingredient> findAllByOrderByNameAscIdAsc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM ingredients WHERE lower(btrim(name)) = lower(btrim(:name)))",
            nativeQuery = true)
    boolean existsByNormalizedName(@Param("name") String name);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM ingredients WHERE lower(btrim(name)) = lower(btrim(:name)) AND id <> :id)",
            nativeQuery = true)
    boolean existsByNormalizedNameAndIdNot(@Param("name") String name, @Param("id") UUID id);
}
