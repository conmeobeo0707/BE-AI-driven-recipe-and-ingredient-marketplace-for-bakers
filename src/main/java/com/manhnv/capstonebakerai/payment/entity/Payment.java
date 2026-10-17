package com.manhnv.capstonebakerai.payment.entity;

import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(columnNames = "external_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentMethod method;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "external_id")            // PayOS orderCode, null với COD
    private String externalId;

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
