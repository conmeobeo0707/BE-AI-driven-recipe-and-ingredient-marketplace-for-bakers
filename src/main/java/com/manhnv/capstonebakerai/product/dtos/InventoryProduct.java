package com.manhnv.capstonebakerai.product.dtos;
import java.math.BigDecimal;
import java.util.UUID;
public record InventoryProduct(UUID productId, BigDecimal stockQuantity, boolean combo) { }
