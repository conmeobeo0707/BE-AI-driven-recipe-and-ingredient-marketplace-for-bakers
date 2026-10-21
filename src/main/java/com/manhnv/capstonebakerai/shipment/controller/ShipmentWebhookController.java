package com.manhnv.capstonebakerai.shipment.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.shipment.dto.request.ShipmentWebhookRequest;
import com.manhnv.capstonebakerai.shipment.service.ShipmentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks/goship")
@RequiredArgsConstructor
@Tag(name = "Vận chuyển")
public class ShipmentWebhookController {

    private final ShipmentWebhookService service;

    @Operation(summary = "Webhook nhận thông tin vận chuyển từ bên đơn vị vận chuyển")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> receive(
            @Valid @RequestBody ShipmentWebhookRequest request) {
        service.handle(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
