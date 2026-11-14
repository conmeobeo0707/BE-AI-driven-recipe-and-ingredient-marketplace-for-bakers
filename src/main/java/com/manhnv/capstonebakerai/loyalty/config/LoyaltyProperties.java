package com.manhnv.capstonebakerai.loyalty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.loyalty")
public class LoyaltyProperties {

    private BigDecimal vndPerPoint = BigDecimal.valueOf(10_000);

    private int maxPointsPerOrder = 500;

    private int firstOrderMultiplier = 2;

    private int welcomePoints = 10;

    private int ingredientImagePrice = 2;

    private int recipeVideoPrice = 5;

    private int purchaseRefundDays = 7;

    private BigDecimal purchaseRefundMinRevenue = BigDecimal.valueOf(100_000);
}
