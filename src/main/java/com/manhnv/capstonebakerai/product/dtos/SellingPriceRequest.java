package com.manhnv.capstonebakerai.product.dtos;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
public record SellingPriceRequest(@NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal sellingPrice) { }
