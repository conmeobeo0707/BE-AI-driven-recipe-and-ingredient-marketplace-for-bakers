package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.payment.entity.Wallet;
import com.manhnv.capstonebakerai.payment.exception.WalletErrorCode;
import com.manhnv.capstonebakerai.payment.mapper.WalletMapper;
import com.manhnv.capstonebakerai.payment.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    @Override
    @Transactional
    public Wallet createWalletForStore(UUID storeId) {
        return walletRepository.findByStoreId(storeId)
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .storeId(storeId)
                            .build();

                    return walletRepository.save(wallet);
                });
    }

    @Override
    public Wallet getWalletByStoreId(UUID storeId) {
        return walletRepository.findByStoreId(storeId)
                .orElseThrow(() -> new AppException(WalletErrorCode.WALLET_NOT_FOUND));

    }
}
