package com.manhnv.capstonebakerai.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.manhnv.capstonebakerai.payment.enums.PayoutStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayoutResponse {
    private UUID payoutId;
    private UUID storeId;
    private BigDecimal amount;
    private PayoutStatus status;
    private UUID processedBy;
    private LocalDateTime createdAt;
    private String rejectReason;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime updatedAt;
}
