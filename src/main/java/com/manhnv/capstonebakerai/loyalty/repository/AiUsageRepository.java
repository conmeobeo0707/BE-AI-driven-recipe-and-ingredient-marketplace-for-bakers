package com.manhnv.capstonebakerai.loyalty.repository;

import com.manhnv.capstonebakerai.loyalty.entity.AiUsage;
import com.manhnv.capstonebakerai.loyalty.enums.AiUsageStatus;
import com.manhnv.capstonebakerai.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {
    // The most expensive use in the time range first, the latest one on a tie
    Optional<AiUsage> findFirstByCustomer_IdAndStatusAndUsedAtBetweenOrderByPointsChargedDescUsedAtDesc(
            UUID customerId, AiUsageStatus status, Instant from, Instant to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AiUsage u
            set u.status = com.manhnv.capstonebakerai.loyalty.enums.AiUsageStatus.REFUNDED_ERROR,
                u.refundedAt = current_timestamp
            where u.id = :id and u.status = com.manhnv.capstonebakerai.loyalty.enums.AiUsageStatus.CHARGED
            """)
    int markRefundedByError(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AiUsage u
            set u.status = com.manhnv.capstonebakerai.loyalty.enums.AiUsageStatus.REFUNDED_PURCHASE,
                u.refundedAt = current_timestamp,
                u.refundOrder = :order
            where u.id = :id and u.status = com.manhnv.capstonebakerai.loyalty.enums.AiUsageStatus.CHARGED
            """)
    int markRefundedByPurchase(@Param("id") UUID id, @Param("order") Order order);
}
