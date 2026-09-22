package com.manhnv.capstonebakerai.payment.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.payment.dto.response.WalletResponse;
import com.manhnv.capstonebakerai.payment.entity.Wallet;
import com.manhnv.capstonebakerai.payment.mapper.WalletMapper;
import com.manhnv.capstonebakerai.payment.repository.WalletRepository;
import com.manhnv.capstonebakerai.payment.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet/{storeId}")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WalletMapper walletMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@PathVariable UUID storeId) {
        WalletResponse response = walletMapper.toWalletResponse(walletService.getWalletByStoreId(storeId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponse> >createStoreWallet(@PathVariable UUID storeId) {
        Wallet wallet = walletService.createWalletForStore(storeId);
        var response = walletMapper.toWalletResponse(wallet);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
