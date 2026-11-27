package com.manhnv.capstonebakerai.product.dtos;
import com.manhnv.capstonebakerai.product.enums.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;
public record InventoryProduct(UUID productId, BigDecimal stockQuantity, boolean combo, ProductStatus status) { }
