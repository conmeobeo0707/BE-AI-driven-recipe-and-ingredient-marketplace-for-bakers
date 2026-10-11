package com.manhnv.capstonebakerai.product.dtos;
import java.time.LocalDateTime;
import java.util.UUID;
public record CategoryResponse(UUID productCategoryId, String categoryName, LocalDateTime createdAt) { }
