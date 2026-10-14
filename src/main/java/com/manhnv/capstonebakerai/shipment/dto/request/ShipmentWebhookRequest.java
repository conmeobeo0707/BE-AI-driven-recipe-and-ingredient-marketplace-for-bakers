package com.manhnv.capstonebakerai.shipment.dto.request;

import com.manhnv.capstonebakerai.shipment.enums.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShipmentWebhookRequest(
        @NotBlank String trackingCode,
        @NotNull Integer statusCode
        ) {
}
