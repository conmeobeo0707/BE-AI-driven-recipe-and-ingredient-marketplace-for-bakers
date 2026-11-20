package com.manhnv.capstonebakerai.loyalty.enums;

//AI_SPEND takes points away; every other type adds them.
public enum LoyaltyTransactionType {
    WELCOME,
    ORDER_EARN,
    AI_SPEND,
    AI_REFUND_ERROR,
    AI_REFUND_PURCHASE
}
