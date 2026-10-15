package com.manhnv.capstonebakerai.shipment.service;

import com.manhnv.capstonebakerai.shipment.dto.request.ShipmentWebhookRequest;

public interface ShipmentWebhookService {
    void handle(ShipmentWebhookRequest request);
}
