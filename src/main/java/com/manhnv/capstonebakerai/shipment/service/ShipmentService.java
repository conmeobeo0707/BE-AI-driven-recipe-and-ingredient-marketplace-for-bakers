package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.shipment.dto.response.ShipmentResponse;

import java.util.UUID;

public interface ShipmentService {
    ShipmentResponse createForOrder(Order order);

    void cancelForOrder(UUID orderId);

    ShipmentResponse getByOrderId(UUID orderId);
}
