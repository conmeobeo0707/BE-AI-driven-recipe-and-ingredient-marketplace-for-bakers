package com.manhnv.capstonebakerai.warehouse.dto;

import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SupplierPaymentResponse(UUID id, String code, UUID supplierId,
        BigDecimal amount, LocalDateTime createdAt, UserSummaryResponse createdBy) { }
