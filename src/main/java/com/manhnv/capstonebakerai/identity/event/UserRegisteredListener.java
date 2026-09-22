package com.manhnv.capstonebakerai.identity.event;

import com.manhnv.capstonebakerai.identity.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredListener.class);
    private final EmailVerificationService emailVerificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            emailVerificationService.sendRegistrationCode(event.userId());
        } catch (RuntimeException exception) {
            log.error("Verification email failed for user {}; resend is available. Failure type: {}",
                    event.userId(), exception.getClass().getSimpleName());
        }
    }
}
