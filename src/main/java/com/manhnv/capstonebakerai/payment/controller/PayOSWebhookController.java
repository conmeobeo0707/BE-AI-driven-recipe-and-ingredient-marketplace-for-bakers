package com.manhnv.capstonebakerai.payment.controller;

import com.manhnv.capstonebakerai.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.model.webhooks.Webhook;

@RestController
@RequestMapping("/api/v1/payos")
@RequiredArgsConstructor
@Tag(name = "Webhook thanh toán")
public class PayOSWebhookController {

    private final PaymentService paymentService;

    @Operation(summary = "Webhook nhận sự kiện customer thanh toán xong")
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Webhook webhook) {
        paymentService.handlePayosWebhook(webhook);
        return ResponseEntity.ok("OK");
    }
}
