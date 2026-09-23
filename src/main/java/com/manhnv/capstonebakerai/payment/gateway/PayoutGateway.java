package com.manhnv.capstonebakerai.payment.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public interface PayoutGateway {

    PayoutTransferResult transfer(PayoutTransferRequest request);

    record PayoutTransferRequest(
            UUID payoutId,
            String bankAccountNumber,
            String bankAccountName,
            String bankCode,
            BigDecimal amount,
            String description
    ) {}

    record PayoutTransferResult(
            boolean success,
            String externalTransactionId,
            String failureReason
    ) {}
}
