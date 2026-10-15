package com.manhnv.capstonebakerai.order.listener;


import com.manhnv.capstonebakerai.order.service.OrderService;
import com.manhnv.capstonebakerai.payment.event.PaymentPaidEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PaymentPaidListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentPaidListener.class);
    private final OrderService orderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentPaid(PaymentPaidEvent event) {
        try {
            orderService.markPaidOrderPending(event.orderId());
        } catch (Exception e) {
            log.error("Auto-confirm after payment failed for order {}", event.orderId(), e);
        }
    }
}
