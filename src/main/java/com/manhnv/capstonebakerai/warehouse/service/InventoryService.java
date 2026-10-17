package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.StockLine;

import java.util.List;
import java.util.UUID;

public interface InventoryService {
    void deductForOrder(UUID orderId, List<StockLine> lines);

    void restockForOrder(UUID orderId);
}
