package com.manhnv.capstonebakerai.voucher.entity;

import com.manhnv.capstonebakerai.voucher.enums.VoucherUsageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "voucher_usages",
        uniqueConstraints = @UniqueConstraint(columnNames = "order_id"),
        indexes = {
                @Index(name = "idx_voucher_usages_voucher_customer", columnList = "voucher_id, customer_id, status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    // Mỗi đơn chỉ có 1 usage (unique)
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoucherUsageStatus status = VoucherUsageStatus.USED;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
}
