package com.manhnv.capstonebakerai.payment.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.payment.dto.response.PayosLinkResponse;
import com.manhnv.capstonebakerai.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Thanh toán")
public class PaymentController {

    private final PaymentService service;

    @Operation(summary = "API tạo link thanh toán", description = "Yêu cầu truyền orderId(sau khi customer order xong lấy orderId đó để tạo link thanh toán")
    @PostMapping("/orders/{orderId}/payos-link")
    public ResponseEntity<ApiResponse<PayosLinkResponse>> createLink(@PathVariable UUID orderId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.createPayosLink(userId, orderId)));
    }
}
