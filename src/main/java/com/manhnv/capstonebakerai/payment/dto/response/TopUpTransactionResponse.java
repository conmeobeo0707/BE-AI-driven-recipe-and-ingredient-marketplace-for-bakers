package com.manhnv.capstonebakerai.payment.dto.response;

import com.manhnv.capstonebakerai.payment.enums.PaymentMethod;
import com.manhnv.capstonebakerai.payment.enums.TopUpStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpTransactionResponse {
    private UUID topupId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String externalTransactionId;
    private TopUpStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
