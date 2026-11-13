package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionRefType;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionType;
import com.manhnv.capstonebakerai.warehouse.dto.StockTransactionFilter;
import com.manhnv.capstonebakerai.warehouse.dto.StockTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface StockTransactionService {
    Page<StockTransactionResponse> search(StockTransactionFilter filter, Pageable pageable);

    void record(UUID stockLotId, UUID productId, StockTransactionType transactionType,
            BigDecimal quantityChange, BigDecimal balanceAfter,
            StockTransactionRefType refType, UUID refId);
}
