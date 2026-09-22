package com.manhnv.capstonebakerai.payment.repository;

import com.manhnv.capstonebakerai.payment.entity.TopUpTransaction;
import com.manhnv.capstonebakerai.payment.enums.TopUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopUpTransactionRepository extends JpaRepository<TopUpTransaction, UUID> {
    Page<TopUpTransaction> findByStoreId(UUID storeId, Pageable pageable);

    Page<TopUpTransaction> findByStoreIdAndStatus(UUID storeId, TopUpStatus status, Pageable pageable);

    Optional<TopUpTransaction> findByExternalTransactionId(String externalTransactionId);
}
