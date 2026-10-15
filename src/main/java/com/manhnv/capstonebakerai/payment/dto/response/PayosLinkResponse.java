package com.manhnv.capstonebakerai.payment.dto.response;

import java.util.UUID;

public record PayosLinkResponse(
        UUID paymentId,
        String checkoutUrl) {
}
