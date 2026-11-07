package com.manhnv.capstonebakerai.warehouse.dto;

import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionRefType;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockTransactionFilter(
        UUID productId,
        UUID stockLotId,
        StockTransactionType transactionType,
        StockTransactionRefType refType,
        UUID refId,
        LocalDateTime from,
        LocalDateTime to) {
}
