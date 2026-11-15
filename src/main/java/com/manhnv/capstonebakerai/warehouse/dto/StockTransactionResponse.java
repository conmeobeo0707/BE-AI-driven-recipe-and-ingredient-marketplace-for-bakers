package com.manhnv.capstonebakerai.warehouse.dto;

import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionRefType;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockTransactionResponse(
        UUID id,
        UUID stockLotId,
        UUID productId,
        StockTransactionType transactionType,
        BigDecimal quantityChange,
        BigDecimal balanceAfter,
        StockTransactionRefType refType,
        UUID refId,
        UUID createdBy,
        LocalDateTime createdAt) {
}
