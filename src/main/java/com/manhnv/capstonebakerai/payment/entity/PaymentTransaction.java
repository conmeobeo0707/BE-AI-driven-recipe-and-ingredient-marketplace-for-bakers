package com.manhnv.capstonebakerai.payment.entity;

import com.manhnv.capstonebakerai.payment.enums.TransactionDirection;
import com.manhnv.capstonebakerai.payment.enums.TransactionMethod;
import com.manhnv.capstonebakerai.payment.enums.TransactionRefType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionRefType refType;

    @Column(nullable = false)
    private UUID refId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionDirection direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private TransactionMethod method;

    private String externalId;   // mã tham chiếu giao dịch ngân hàng (reference của PayOS)
    private String note;
    private UUID createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
