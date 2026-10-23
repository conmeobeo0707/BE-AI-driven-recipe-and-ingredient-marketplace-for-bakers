package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeInteraction;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RecipeInteractionRepository
        extends JpaRepository<RecipeInteraction, UUID> {

    Page<RecipeInteraction> findByCustomer_IdOrderByOccurredAtDesc(UUID customerId, Pageable pageable);

    Page<RecipeInteraction> findByCustomer_IdAndInteractionTypeOrderByOccurredAtDesc(
            UUID customerId,
            RecipeInteractionType interactionType,
            Pageable pageable
    );
}
