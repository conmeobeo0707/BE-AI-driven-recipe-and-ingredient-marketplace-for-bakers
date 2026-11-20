package com.manhnv.capstonebakerai.loyalty.dto;

import com.manhnv.capstonebakerai.loyalty.entity.LoyaltyTransaction;
import com.manhnv.capstonebakerai.loyalty.enums.LoyaltyTransactionType;

import java.time.Instant;
import java.util.UUID;

public record LoyaltyTransactionResponse(
        UUID id,
        LoyaltyTransactionType type,
        int points,
        int balanceAfter,
        String note,
        UUID orderId,
        Instant createdAt
) {

    public static LoyaltyTransactionResponse from(LoyaltyTransaction transaction) {
        return new LoyaltyTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getPoints(),
                transaction.getBalanceAfter(),
                transaction.getNote(),
                transaction.getOrder() == null ? null : transaction.getOrder().getId(),
                transaction.getCreatedAt()
        );
    }
}
