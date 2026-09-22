package com.manhnv.capstonebakerai.payment.repository;

import com.manhnv.capstonebakerai.payment.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByStoreId(UUID storeId);
    boolean existsByStoreId(UUID storeId);
}
