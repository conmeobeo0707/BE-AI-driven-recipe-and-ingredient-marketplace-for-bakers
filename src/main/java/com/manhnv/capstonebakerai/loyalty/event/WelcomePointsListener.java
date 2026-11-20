package com.manhnv.capstonebakerai.loyalty.event;

import com.manhnv.capstonebakerai.identity.event.EmailVerifiedEvent;
import com.manhnv.capstonebakerai.loyalty.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WelcomePointsListener {

    private static final Logger log = LoggerFactory.getLogger(WelcomePointsListener.class);

    private final LoyaltyService loyaltyService;

    // Runs after the verification is committed
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerified(EmailVerifiedEvent event) {
        try {
            loyaltyService.grantWelcomePoints(event.customerId());
        } catch (RuntimeException exception) {
            log.error("Welcome points failed for customer {}. Failure type: {}",
                    event.customerId(), exception.getClass().getSimpleName());
        }
    }
}
