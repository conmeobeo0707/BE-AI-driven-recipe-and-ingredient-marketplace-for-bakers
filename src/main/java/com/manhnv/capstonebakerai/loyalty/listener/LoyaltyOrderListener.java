package com.manhnv.capstonebakerai.loyalty.listener;

import com.manhnv.capstonebakerai.loyalty.service.LoyaltyService;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.sqm.mutation.internal.UpdateHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoyaltyOrderListener {

    private final LoyaltyService loyaltyService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.newStatus() != OrderStatus.DELIVERED) return;
        try {
            loyaltyService.handleOrderDelivered(event.orderId());
        } catch (Exception e) {
            log.error("Cộng điểm đơn {} thất bại, cần xử lý lại", event.orderId(), e);
        }
    }
}
