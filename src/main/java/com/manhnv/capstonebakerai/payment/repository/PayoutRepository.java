package com.manhnv.capstonebakerai.payment.repository;

import com.manhnv.capstonebakerai.payment.entity.Payout;
import com.manhnv.capstonebakerai.payment.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    Page<Payout> findByStoreId(UUID storeId, Pageable pageable);

    Page<Payout> findByStatus(PayoutStatus status, Pageable pageable);

    Page<Payout> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Payout> findByPayoutId(UUID payoutId);
}
