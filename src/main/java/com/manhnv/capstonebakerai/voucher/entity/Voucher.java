package com.manhnv.capstonebakerai.voucher.entity;

import com.manhnv.capstonebakerai.voucher.enums.DiscountType;
import com.manhnv.capstonebakerai.voucher.enums.VoucherDisplayStatus;
import com.manhnv.capstonebakerai.voucher.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@DynamicUpdate
@Table(name = "vouchers",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"),
        indexes = {
                @Index(name = "idx_vouchers_status_dates", columnList = "status, start_date, end_date")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    // PERCENT: 0-100, FIXED_AMOUNT: số tiền
    @Column(name = "discount_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountValue;

    // Trần giảm, chỉ có ý nghĩa với PERCENT
    @Column(name = "max_discount_amount", precision = 19, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "used_quantity", nullable = false)
    @Builder.Default
    private Integer usedQuantity = 0;

    @Column(name = "max_usage_per_customer", nullable = false)
    @Builder.Default
    private Integer maxUsagePerCustomer = 1;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    // userId của manager tạo
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /** Số tiền giảm trên tiền hàng `base`. Không bao giờ vượt quá `base`. */
    public BigDecimal calculateDiscount(BigDecimal base) {
        BigDecimal discount;
        if (discountType == DiscountType.PERCENT) {
            discount = base.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = discountValue;
        }
        return discount.min(base);
    }

    public VoucherDisplayStatus displayStatus(LocalDateTime now) {
        if (status == VoucherStatus.INACTIVE) return VoucherDisplayStatus.DISABLED;
        if (now.isBefore(startDate)) return VoucherDisplayStatus.UPCOMING;
        if (now.isAfter(endDate)) return VoucherDisplayStatus.EXPIRED;
        if (usedQuantity >= totalQuantity) return VoucherDisplayStatus.OUT_OF_STOCK;
        return VoucherDisplayStatus.RUNNING;
    }

    public int remainingQuantity() {
        return Math.max(0, totalQuantity - usedQuantity);
    }
}
