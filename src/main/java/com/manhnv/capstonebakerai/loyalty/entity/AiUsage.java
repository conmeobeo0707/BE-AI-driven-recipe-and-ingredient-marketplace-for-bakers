package com.manhnv.capstonebakerai.loyalty.entity;

import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.loyalty.enums.AiFeature;
import com.manhnv.capstonebakerai.loyalty.enums.AiUsageStatus;
import com.manhnv.capstonebakerai.order.entity.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usages")
@Getter
@Setter
@NoArgsConstructor
public class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature", nullable = false, length = 30, updatable = false)
    private AiFeature feature;

    // Stored so the refund gives back this amount even if the price changes later.
    @Column(name = "points_charged", nullable = false, updatable = false)
    private int pointsCharged;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiUsageStatus status = AiUsageStatus.CHARGED;

    // The order that made this use refundable; set only with REFUNDED_PURCHASE.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_order_id")
    private Order refundOrder;

    @Column(name = "used_at", nullable = false, updatable = false)
    private Instant usedAt = Instant.now();

    @Column(name = "refunded_at")
    private Instant refundedAt;
}
