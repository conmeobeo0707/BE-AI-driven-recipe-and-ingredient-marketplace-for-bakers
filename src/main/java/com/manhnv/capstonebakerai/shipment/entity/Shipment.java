package com.manhnv.capstonebakerai.shipment.entity;

import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.shipment.enums.ShipmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipments",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "order_id"),
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;            // GOSHIP | FAKE

    @Column(name = "carrier", length = 50)
    private String carrier;             // hãng vận chuyển khách chọn (3B sẽ dùng)

    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;

    // COD: số tiền shipper thu hộ; PAYOS: 0
    @Column(name = "cod_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal codAmount;

    @Column(name = "weight_gram", nullable = false)
    private int weightGram;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
