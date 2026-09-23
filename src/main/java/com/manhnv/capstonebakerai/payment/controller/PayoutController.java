package com.manhnv.capstonebakerai.payment.controller;

import com.manhnv.capstonebakerai.payment.dto.request.PayoutRejectRequest;
import com.manhnv.capstonebakerai.payment.dto.request.PayoutRequest;
import com.manhnv.capstonebakerai.payment.dto.response.PayoutResponse;
import com.manhnv.capstonebakerai.payment.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    // Store tạo yêu cầu rút tiền
    @PostMapping("/stores/{storeId}")
    public ResponseEntity<PayoutResponse> createPayoutRequest(@PathVariable UUID storeId, @Valid @RequestBody PayoutRequest request) {
        return ResponseEntity.ok(payoutService.createPayoutRequest(storeId, request));
    }

    // Admin duyệt payout -> tự động kích hoạt thanh toán
    @PutMapping("/{payoutId}/approve")
    // TODO: thêm @PreAuthorize("hasRole('ADMIN')") khi có security config
    public ResponseEntity<PayoutResponse> approvePayout(@PathVariable UUID payoutId) {
        return ResponseEntity.ok(payoutService.approvePayout(payoutId));
    }

    // Admin từ chối payout -> hoàn tiền về availableBalance
    @PutMapping("/{payoutId}/reject")
    // TODO: thêm @PreAuthorize("hasRole('ADMIN')") khi có security config
    public ResponseEntity<PayoutResponse> rejectPayout(@PathVariable UUID payoutId, @Valid @RequestBody PayoutRejectRequest request) {
        return ResponseEntity.ok(payoutService.rejectPayout(payoutId, request));
    }
}