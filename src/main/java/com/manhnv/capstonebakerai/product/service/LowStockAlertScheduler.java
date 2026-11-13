package com.manhnv.capstonebakerai.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockAlertScheduler {
    private final LowStockAlertService lowStockAlertService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void sendLowStockAlertsHourly() {
        try {
            var summary = lowStockAlertService.sendLowStockAlerts();
            if (summary.notificationsSent() > 0) {
                log.info("Sent {} low-stock notifications for {} products",
                        summary.notificationsSent(), summary.lowStockProducts());
            }
        } catch (Exception exception) {
            log.error("Failed to send scheduled low-stock alerts", exception);
        }
    }
}
