package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;
import com.manhnv.capstonebakerai.warehouse.entity.StocktakeItem;
import com.manhnv.capstonebakerai.warehouse.enums.LotStatus;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionRefType;
import com.manhnv.capstonebakerai.warehouse.enums.StockTransactionType;
import com.manhnv.capstonebakerai.warehouse.exception.WarehouseErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

final class StocktakeSurplus {
    private StocktakeSurplus() { }

    // The caller locks the stocktake, product and lot in that order.
    static void post(UUID noteId, StocktakeItem count, StockLot lot, LocalDate today,
                     StockTransactionService transactions, Map<UUID, BigDecimal> stockDeltas) {
        if (lot == null || !count.getProductId().equals(lot.getProductId())
                || (lot.getStatus() != LotStatus.STORAGE && lot.getStatus() != LotStatus.EXPIRED)
                || lot.getCurrentQuantity().compareTo(count.getSystemQuantity()) != 0) {
            throw new AppException(WarehouseErrorCode.CONFLICT, "Surplus lot changed during stocktake");
        }

        BigDecimal increase = count.getDiffQuantity();
        BigDecimal balance = lot.getCurrentQuantity().add(increase);
        lot.setCurrentQuantity(balance);
        if (lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(today)) {
            lot.setStatus(LotStatus.EXPIRED);
        }
        stockDeltas.merge(count.getProductId(), increase, BigDecimal::add);
        transactions.record(lot.getLotId(), count.getProductId(), StockTransactionType.STOCKTAKE_INCREASE,
                increase, balance, StockTransactionRefType.STOCKTAKE_NOTE, noteId);
    }
}
