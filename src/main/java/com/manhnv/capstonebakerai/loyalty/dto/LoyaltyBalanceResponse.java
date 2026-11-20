package com.manhnv.capstonebakerai.loyalty.dto;

public record LoyaltyBalanceResponse(
        int balance,
        int ingredientImagePrice,
        int recipeVideoPrice
) {
}
