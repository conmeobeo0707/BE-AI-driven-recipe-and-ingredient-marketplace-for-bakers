package com.manhnv.capstonebakerai.shipment.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.shipment.dto.request.ShipmentWebhookRequest;
import com.manhnv.capstonebakerai.shipment.service.ShipmentWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/goship")
@RequiredArgsConstructor
public class ShipmentWebhookController {

    private final ShipmentWebhookService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> receive(
            @RequestHeader(value = "X-Webhook-Token", required = false) String token,
            @Valid @RequestBody ShipmentWebhookRequest request) {
        service.handle(token, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
