package com.manhnv.capstonebakerai.warehouse.service;

import com.manhnv.capstonebakerai.warehouse.dto.AvailableInventoryResponse;
import com.manhnv.capstonebakerai.warehouse.dto.StockReservationCreateRequest;
import com.manhnv.capstonebakerai.warehouse.dto.StockReservationResponse;
import com.manhnv.capstonebakerai.warehouse.entity.StockOutNote;
import java.util.UUID;

public interface StockReservationService {
    StockReservationResponse reserve(StockReservationCreateRequest request);
    StockReservationResponse getByOrderId(UUID orderId);
    StockReservationResponse release(UUID orderId);
    StockOutNote consume(UUID orderId);
    AvailableInventoryResponse getAvailableInventory(UUID productId);
    int expirePastDueReservations();
}
