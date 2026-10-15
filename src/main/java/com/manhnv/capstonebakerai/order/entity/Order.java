package com.manhnv.capstonebakerai.order.entity;

import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(columnNames = "code"),
        indexes = {
                @Index(name = "idx_orders_customer_status", columnList = "customer_id, status"),
                @Index(name = "idx_orders_created_at", columnList = "created_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // userId của người tạo: chính khách hoặc staff tạo hộ
    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "products_price", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal productsPrice = BigDecimal.ZERO;

    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    // Chỉ có giá trị khi DELIVERED
    @Column(name = "revenue", precision = 19, scale = 2)
    private BigDecimal revenue;

    @Column(name = "note", length = 500)
    private String note;

    // ===== Snapshot địa chỉ nhận hàng (Phase 3 sẽ validate theo mã Goship) =====
    @Column(name = "receiver_name", length = 150)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "province_code", length = 20)
    private String provinceCode;

    @Column(name = "district_code", length = 20)
    private String districtCode;

    @Column(name = "ward_code", length = 20)
    private String wardCode;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "full_address", length = 500)
    private String fullAddress;

    // ===== Thông tin hủy / nhận hàng =====
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "shipping_rate_id", length = 100)
    private String shippingRateId;

    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Column(name = "shipping_service", length = 100)
    private String shippingService;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "received_confirmed_at")
    private LocalDateTime receivedConfirmedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    /** Tính lại các khoản tiền từ productsPrice, discountPercent, shippingFee. */
    public void recalculateTotals() {
        this.discountAmount = productsPrice
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        this.total = productsPrice.subtract(discountAmount).add(shippingFee);
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
    }

    public int totalWeightGram() {
        return items.stream().mapToInt(i -> i.getWeightGram() * i.getQuantity()).sum();
    }
}
