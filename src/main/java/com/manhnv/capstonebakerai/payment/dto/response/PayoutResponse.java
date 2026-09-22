package com.manhnv.capstonebakerai.payment.dto.response;

import com.manhnv.capstonebakerai.payment.enums.PayoutStatus;
import com.manhnv.capstonebakerai.payment.enums.TopUpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutResponse {
    private UUID payoutId;
    private UUID storeId;
    private BigDecimal amount;
    private PayoutStatus status;
    private UUID processedBy;
    private String checkoutUrl;
    private LocalDateTime createdAt;
}
