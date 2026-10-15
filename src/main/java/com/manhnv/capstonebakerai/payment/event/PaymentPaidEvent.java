package com.manhnv.capstonebakerai.payment.event;

import java.util.UUID;

public record PaymentPaidEvent(UUID orderId) {
}
