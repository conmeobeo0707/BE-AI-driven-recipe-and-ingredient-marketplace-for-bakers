package com.manhnv.capstonebakerai.payment.service;

import com.manhnv.capstonebakerai.payment.dto.response.WalletResponse;
import com.manhnv.capstonebakerai.payment.entity.Wallet;

import java.util.UUID;

public interface WalletService {

    Wallet createWalletForStore(UUID storeId);

    Wallet getWalletByStoreId(UUID storeId);
}
