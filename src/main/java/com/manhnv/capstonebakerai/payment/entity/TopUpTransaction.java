package com.manhnv.capstonebakerai.payment.entity;

import com.manhnv.capstonebakerai.payment.enums.PaymentMethod;
import com.manhnv.capstonebakerai.payment.enums.TopUpStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "topup_transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "topup_id", updatable = false, nullable = false)
    private UUID topupId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "external_transaction_id")
    private String externalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TopUpStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
