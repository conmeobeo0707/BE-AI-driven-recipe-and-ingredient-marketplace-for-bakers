package com.manhnv.capstonebakerai.identity.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.manhnv.capstonebakerai.identity.service.EmailTemplateService;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PasswordResetMailListener {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailListener.class);

    private final EmailTemplateService emailTemplateService;

    @Value("${app.password-reset.code-expiration}")
    private Duration codeExpiration;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            emailTemplateService.send(
                    event.email(),
                    "Mã đặt lại mật khẩu Baker AI",
                    "password-reset.html",
                    "Mã đặt lại mật khẩu của bạn là " + event.code() + ". Mã có hiệu lực trong "
                            + codeExpiration.toMinutes() + " phút.",
                    Map.of("code", event.code(), "expirationMinutes", String.valueOf(codeExpiration.toMinutes()))
            );
        } catch (RuntimeException exception) {
            log.error("Password reset email failed for user {}; request a new code after the cooldown. Failure type: {}",
                    event.userId(), exception.getClass().getSimpleName());
        }
    }
}
