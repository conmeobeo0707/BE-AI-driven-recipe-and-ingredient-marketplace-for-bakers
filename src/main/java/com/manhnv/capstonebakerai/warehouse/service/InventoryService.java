package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.StockLine;
import com.manhnv.capstonebakerai.warehouse.entity.StockLot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InventoryService {
    void reserveForOrder(UUID orderId, List<StockLine> lines, LocalDateTime expiresAt);
    void releaseOrderReservation(UUID orderId);
    void deductForOrder(UUID orderId, List<StockLine> lines);

    void restockForOrder(UUID orderId);
    List<StockLot> getExpiringLots(int withinDays);
    List<StockLot> getExpiredLots();
}
