package com.manhnv.capstonebakerai.shipment.dto.response;

import com.manhnv.capstonebakerai.shipment.enums.ShipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID orderId,
        String provider,
        String carrier,
        String trackingCode,
        ShipmentStatus status,
        BigDecimal shippingFee,
        BigDecimal codAmount,
        int weightGram,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime createdAt
) {
}
