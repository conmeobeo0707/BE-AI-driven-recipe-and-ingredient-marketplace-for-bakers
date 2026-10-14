package com.manhnv.capstonebakerai.shipment.provider;

import com.manhnv.capstonebakerai.order.entity.Order;

import java.math.BigDecimal;

public interface ShippingProvider {
    String name();

    ShipmentCreateResult createShipment(Order order, BigDecimal codAmount, int weightGram);

    void cancelShipment(String externalId);

    record ShipmentCreateResult(String externalId, String trackingCode, String carrier) {}
}
