package com.manhnv.capstonebakerai.warehouse.dto;

import com.manhnv.capstonebakerai.warehouse.enums.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StockReservationResponse(UUID reservationId, UUID orderId, ReservationStatus status,
                                       LocalDateTime expiresAt, List<Item> items) {
    public record Item(UUID productId, UUID lotId, UUID unitId, BigDecimal quantity) { }
}
