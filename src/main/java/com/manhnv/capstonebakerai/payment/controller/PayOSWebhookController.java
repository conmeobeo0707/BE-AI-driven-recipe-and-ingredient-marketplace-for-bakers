package com.manhnv.capstonebakerai.payment.controller;

import com.manhnv.capstonebakerai.payment.service.TopUpService;
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
public class PayOSWebhookController {

    private final TopUpService topUpService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Webhook webhook) {
        topUpService.handleWebhook(webhook);
        return ResponseEntity.ok("OK");
    }
}
