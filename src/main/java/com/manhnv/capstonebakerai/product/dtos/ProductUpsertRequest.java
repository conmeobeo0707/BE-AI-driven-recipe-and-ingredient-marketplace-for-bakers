package com.manhnv.capstonebakerai.product.dtos;

import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpsertRequest(String sku, String name, String description, UUID categoryId,
        String imgUrl, BigDecimal discountPercent, String storageGuide, BigDecimal minStock,
        BigDecimal maxStock, ProductStatus status, String brand, Boolean combo) { }
