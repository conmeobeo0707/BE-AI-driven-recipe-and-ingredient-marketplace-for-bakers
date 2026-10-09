package com.manhnv.capstonebakerai.product.dtos;

import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponse(UUID productId, String sku, String name, String description,
        UUID categoryId, String imgUrl, BigDecimal discountPercent, String storageGuide,
        BigDecimal minStock, BigDecimal maxStock, BigDecimal stockQuantity, ProductStatus status,
        String brand, boolean combo, LocalDateTime createdAt, LocalDateTime updatedAt,
        List<ProductUnitResponse> units) { }
