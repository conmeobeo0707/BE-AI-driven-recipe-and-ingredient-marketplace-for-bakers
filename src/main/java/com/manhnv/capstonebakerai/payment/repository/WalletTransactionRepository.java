package com.manhnv.capstonebakerai.payment.repository;

import com.manhnv.capstonebakerai.payment.entity.WalletTransaction;
import com.manhnv.capstonebakerai.payment.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    Page<WalletTransaction> findByWallet_StoreWalletId(UUID storeWalletId, Pageable pageable);

    Page<WalletTransaction> findByWallet_StoreWalletIdAndTransactionType(UUID storeWalletId, TransactionType type, Pageable pageable);
}
