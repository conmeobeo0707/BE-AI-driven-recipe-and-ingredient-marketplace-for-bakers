package com.manhnv.capstonebakerai.product.dtos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
public record UnitPriceHistoryResponse(UUID unitHistoryId, UUID unitId, UUID updatedBy,
        BigDecimal sellingPrice, LocalDateTime createdAt) { }
