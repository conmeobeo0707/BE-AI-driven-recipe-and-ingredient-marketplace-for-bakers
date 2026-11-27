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
            throw new AppException(WarehouseErrorCode.CONFLICT, "Lot changed during stocktake");
        }

        BigDecimal difference = count.getDiffQuantity();
        BigDecimal balance = lot.getCurrentQuantity().add(difference);
        lot.setCurrentQuantity(balance);
        if (balance.signum() == 0) {
            lot.setStatus(LotStatus.EMPTY);
        } else if (lot.getExpiryDate() != null && lot.getExpiryDate().isBefore(today)) {
            lot.setStatus(LotStatus.EXPIRED);
        }
        stockDeltas.merge(count.getProductId(), difference, BigDecimal::add);
        StockTransactionType transactionType = difference.signum() > 0
                ? StockTransactionType.STOCKTAKE_INCREASE : StockTransactionType.STOCKTAKE_DECREASE;
        transactions.record(lot.getLotId(), count.getProductId(), transactionType,
                difference, balance, StockTransactionRefType.STOCKTAKE_NOTE, noteId);
    }
}
