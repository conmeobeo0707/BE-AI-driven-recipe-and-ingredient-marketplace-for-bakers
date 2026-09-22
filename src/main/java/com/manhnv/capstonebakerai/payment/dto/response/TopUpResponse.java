package com.manhnv.capstonebakerai.payment.dto.response;

import com.manhnv.capstonebakerai.payment.enums.TopUpStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUpResponse{
    private UUID topUpId;
    private UUID storeId;
    private BigDecimal amount;
    private TopUpStatus status;
    private String checkoutUrl;
    private LocalDateTime createdAt;
}
