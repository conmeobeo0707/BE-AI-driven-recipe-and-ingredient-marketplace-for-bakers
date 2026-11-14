package com.manhnv.capstonebakerai.loyalty.service;

import com.manhnv.capstonebakerai.loyalty.enums.AiFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class PaidAiRunner {

    private static final String DEFAULT_REFUND_REASON = "AI gặp lỗi";

    private final LoyaltyService loyaltyService;

    public <T> T run(UUID customerId, AiFeature feature, Supplier<T> aiCall,
                     Predicate<RuntimeException> keepPoints) {
        UUID usageId = loyaltyService.chargeForAi(customerId, feature);
        try {
            return aiCall.get();
        } catch (RuntimeException error) {
            if (!keepPoints.test(error)) {
                loyaltyService.refundAiError(usageId, DEFAULT_REFUND_REASON);
            }
            throw error;
        }
    }
}
