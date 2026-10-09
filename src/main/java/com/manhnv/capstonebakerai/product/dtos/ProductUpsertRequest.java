package com.manhnv.capstonebakerai.product.dtos;

import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpsertRequest(
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 255) String name,
        String description,
        UUID categoryId,
        String imgUrl,
        @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer = 3, fraction = 2) BigDecimal discountPercent,
        String storageGuide,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal minStock,
        @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal maxStock,
        ProductStatus status,
        @Size(max = 255) String brand,
        Boolean combo) { }
