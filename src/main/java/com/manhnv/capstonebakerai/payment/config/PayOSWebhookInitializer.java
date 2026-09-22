package com.manhnv.capstonebakerai.payment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vn.payos.PayOS;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayOSWebhookInitializer implements CommandLineRunner {

    private final PayOS payOS;

    @Value("${payos.webhook-url}")
    private String webhookUrl;


    @Override
    public void run(String... args) throws Exception {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            payOS.webhooks().confirm(webhookUrl);
            log.info("PayOS webhook confirmed: {}", webhookUrl);
        } catch (Exception e) {
            log.warn("Failed to confirm PayOS webhook: {}", e.getMessage());
        }
    }
}
