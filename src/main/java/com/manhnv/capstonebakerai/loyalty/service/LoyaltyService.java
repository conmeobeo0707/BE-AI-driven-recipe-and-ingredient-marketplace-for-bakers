package com.manhnv.capstonebakerai.loyalty.service;

import com.manhnv.capstonebakerai.loyalty.entity.LoyaltyTransaction;
import com.manhnv.capstonebakerai.loyalty.enums.AiFeature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface LoyaltyService {

    int getBalance(UUID customerId);

    Page<LoyaltyTransaction> getTransactions(UUID customerId, Pageable pageable);

    int priceOf(AiFeature feature);

    int expectedOrderPoints(UUID customerId, BigDecimal revenue);

    boolean grantWelcomePoints(UUID customerId);

    void handleOrderDelivered(UUID orderId);

    UUID chargeForAi(UUID customerId, AiFeature feature);

    boolean refundAiError(UUID aiUsageId, String reason);
}
