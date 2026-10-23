package com.manhnv.capstonebakerai.recommendation.service;

import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.entity.RecipeInteraction;
import com.manhnv.capstonebakerai.recipe.entity.RecipeBookmark;
import com.manhnv.capstonebakerai.recipe.entity.RecipeRating;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.recipe.repository.RecipeBookmarkRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeInteractionRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRatingRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionRecordedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.recombee", name = "backfill-on-startup", havingValue = "true")
public class RecombeeBackfillRunner implements ApplicationRunner {

    private final RecipeRepository recipeRepository;
    private final CustomerRepository customerRepository;
    private final RecipeInteractionRepository interactionRepository;
    private final RecipeBookmarkRepository bookmarkRepository;
    private final RecipeRatingRepository ratingRepository;
    private final RecombeeSyncService syncService;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        recipeRepository.findAll().forEach(recipe -> runSafely("recipe " + recipe.getId(),
                () -> syncService.syncRecipe(recipe.getId())));
        customerRepository.findAll().forEach(customer -> runSafely("customer " + customer.getId(),
                () -> syncService.syncCustomer(customer.getId())));
        interactionRepository.findAll().forEach(this::syncInteraction);
        bookmarkRepository.findAll().forEach(this::syncBookmark);
        ratingRepository.findAll().forEach(this::syncRating);
        log.info("Recombee catalog/profile/interaction backfill completed");
    }

    private void syncInteraction(RecipeInteraction interaction) {
        if (interaction.getInteractionType() != RecipeInteractionType.VIEW || interaction.getRecipe() == null) return;
        RecipeInteractionRecordedEvent event = new RecipeInteractionRecordedEvent(
                interaction.getId(),
                interaction.getCustomer().getId(),
                interaction.getRecipe().getId(),
                interaction.getInteractionType(),
                interaction.getRatingScore(),
                interaction.getRecommId(),
                interaction.getOccurredAt()
        );
        runSafely("interaction " + interaction.getId(), () -> syncService.syncInteraction(event));
    }

    private void syncBookmark(RecipeBookmark bookmark) {
        RecipeInteractionRecordedEvent event = new RecipeInteractionRecordedEvent(
                bookmark.getId(),
                bookmark.getCustomer().getId(),
                bookmark.getRecipe().getId(),
                RecipeInteractionType.BOOKMARK,
                null,
                null,
                toInstant(bookmark.getCreatedAt())
        );
        runSafely("bookmark " + bookmark.getId(), () -> syncService.syncInteraction(event));
    }

    private void syncRating(RecipeRating rating) {
        RecipeInteractionRecordedEvent event = new RecipeInteractionRecordedEvent(
                rating.getId(),
                rating.getCustomer().getId(),
                rating.getRecipe().getId(),
                RecipeInteractionType.RATING,
                rating.getScore(),
                null,
                toInstant(rating.getCreatedAt())
        );
        runSafely("rating " + rating.getId(), () -> syncService.syncInteraction(event));
    }

    private void runSafely(String record, ThrowingAction action) {
        try {
            action.run();
        } catch (Exception exception) {
            log.warn("Could not backfill {} to Recombee", record, exception);
        }
    }

    private Instant toInstant(LocalDateTime timestamp) {
        return timestamp == null ? Instant.now() : timestamp.toInstant(ZoneOffset.UTC);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
