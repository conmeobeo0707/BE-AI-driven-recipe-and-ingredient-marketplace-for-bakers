package com.manhnv.capstonebakerai.voucher.repository;

import com.manhnv.capstonebakerai.voucher.entity.VoucherUsage;
import com.manhnv.capstonebakerai.voucher.enums.VoucherStatus;
import com.manhnv.capstonebakerai.voucher.enums.VoucherUsageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, UUID> {
    long countByVoucherIdAndCustomerIdAndStatus(UUID voucherId, UUID customerId, VoucherUsageStatus status);

    boolean existsByVoucherId(UUID voucherId);

    Optional<VoucherUsage> findByOrderIdAndStatus(UUID orderId, VoucherUsageStatus status);

    Page<VoucherUsage> findByVoucherId(UUID voucherId, Pageable pageable);
}
