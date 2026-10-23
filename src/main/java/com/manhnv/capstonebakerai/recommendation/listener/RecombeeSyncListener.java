package com.manhnv.capstonebakerai.recommendation.listener;

import com.manhnv.capstonebakerai.recommendation.event.CustomerProfileChangedEvent;
import com.manhnv.capstonebakerai.recommendation.event.RecipeChangedEvent;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionDeletedEvent;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionRecordedEvent;
import com.manhnv.capstonebakerai.recommendation.service.RecombeeSyncService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecombeeSyncListener {
    private final RecombeeSyncService syncService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecipeChanged(RecipeChangedEvent event) {
        try {
            syncService.syncRecipe(event.recipeId());
        } catch (Exception ex) {
            log.warn("Could not sync recipe {} to Recombee", event.recipeId(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCustomerProfileChanged(CustomerProfileChangedEvent event) {
        try {
            syncService.syncCustomer(event.customerId());
        } catch (Exception ex) {
            log.warn("Could not sync customer {} to Recombee", event.customerId(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInteractionRecorded(RecipeInteractionRecordedEvent event) {
        try {
            syncService.syncInteraction(event);
        } catch (Exception ex) {
            log.warn("Could not sync interaction {} to Recombee", event.interactionId(), ex);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInteractionDeleted(RecipeInteractionDeletedEvent event) {
        try {
            syncService.deleteInteraction(event);
        } catch (Exception ex) {
            log.warn("Could not delete interaction for customer {} and recipe {} in Recombee",
                    event.customerId(), event.recipeId(), ex);
        }
    }
}
