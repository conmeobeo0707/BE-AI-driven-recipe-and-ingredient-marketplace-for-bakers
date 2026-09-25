package com.manhnv.capstonebakerai.identity.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class PasswordResetMailListener {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailListener.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String sender;

    @Value("${app.password-reset.code-expiration}")
    private Duration codeExpiration;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(event.email());
        message.setSubject("Your password reset code");
        message.setText("Your password reset code is %s. It expires in %d minutes."
                .formatted(event.code(), codeExpiration.toMinutes()));

        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            log.error("Password reset email failed for user {}; request a new code after the cooldown. Failure type: {}",
                    event.userId(), exception.getClass().getSimpleName());
        }
    }
}
