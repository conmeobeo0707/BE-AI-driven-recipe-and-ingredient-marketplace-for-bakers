package com.manhnv.capstonebakerai.payment.dto.response;

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
public class WalletResponse {

    UUID storeWalletId;
    UUID storeId;
    BigDecimal totalBalance;
    BigDecimal lockedBalance;
    BigDecimal availableBalance;
    LocalDateTime createdAt;
}
