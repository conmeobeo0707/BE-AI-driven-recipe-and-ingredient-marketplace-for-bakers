package com.manhnv.capstonebakerai.product.dtos;
import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
public record UnitPriceHistoryResponse(UUID unitHistoryId, UUID unitId, UserSummaryResponse updatedBy,
        BigDecimal sellingPrice, LocalDateTime createdAt) { }
