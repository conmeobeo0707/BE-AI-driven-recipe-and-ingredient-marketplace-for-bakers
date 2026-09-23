package com.manhnv.capstonebakerai.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotImplementedPayoutGateway implements PayoutGateway {

    @Override
    public PayoutTransferResult transfer(PayoutTransferRequest request) {
        log.warn("PayoutGateway not implemented yet, payoutId={}", request.payoutId());
        throw new UnsupportedOperationException("Payout gateway integration is not implemented yet for payoutId=" + request.payoutId());
    }
}
