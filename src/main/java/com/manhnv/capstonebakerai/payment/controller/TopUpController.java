package com.manhnv.capstonebakerai.payment.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.payment.dto.request.TopUpRequest;
import com.manhnv.capstonebakerai.payment.dto.response.TopUpResponse;
import com.manhnv.capstonebakerai.payment.dto.response.TopUpTransactionResponse;
import com.manhnv.capstonebakerai.payment.service.TopUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment/{storeId}/topup-transactions")
@RequiredArgsConstructor
public class TopUpController {

    private final TopUpService topUpService;

    @PostMapping
    public ResponseEntity<ApiResponse<TopUpResponse>> createTopUp(
            @PathVariable UUID storeId,
            @Valid @RequestBody TopUpRequest request
            ) {
        TopUpResponse response = topUpService.createTopUp(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TopUpTransactionResponse>>> getHistory(
            @PathVariable UUID storeId,
            Pageable pageable) {
        Page<TopUpTransactionResponse> response = topUpService.getTopUpHistory(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{topUpId}")
    public ResponseEntity<ApiResponse<TopUpTransactionResponse>> getById(
            @PathVariable UUID storeId,
            @PathVariable UUID topUpId) {
        TopUpTransactionResponse response = topUpService.getTopUpById(topUpId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
