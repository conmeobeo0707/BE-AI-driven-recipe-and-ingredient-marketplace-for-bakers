package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeInteraction;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface RecipeInteractionRepository
        extends JpaRepository<RecipeInteraction, UUID> {

    Page<RecipeInteraction> findByCustomer_IdOrderByOccurredAtDesc(UUID customerId, Pageable pageable);

    Page<RecipeInteraction> findByCustomer_IdAndInteractionTypeOrderByOccurredAtDesc(
            UUID customerId,
            RecipeInteractionType interactionType,
            Pageable pageable
    );

    boolean existsByCustomer_IdAndRecipe_IdAndInteractionTypeAndOccurredAtAfter(
            UUID customerId,
            UUID recipeId,
            RecipeInteractionType interactionType,
            Instant after
    );

    // The newest row of each (recipe, type) for this customer.
    @Query("""
            select i.id from RecipeInteraction i
            where i.customer.id = :customerId
              and i.recipe.id in :recipeIds
              and i.interactionType in :types
              and i.occurredAt = (
                  select max(j.occurredAt) from RecipeInteraction j
                  where j.customer = i.customer
                    and j.recipe = i.recipe
                    and j.interactionType = i.interactionType)
            """)
    Set<UUID> findLatestIds(
            @Param("customerId") UUID customerId,
            @Param("recipeIds") Collection<UUID> recipeIds,
            @Param("types") Collection<RecipeInteractionType> types
    );
}
