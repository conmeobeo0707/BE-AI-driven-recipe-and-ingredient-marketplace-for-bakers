package com.manhnv.capstonebakerai.shipment.dto.response;

import java.math.BigDecimal;

public record ShippingRateResponse(
        String id,
        String carrierName,
        String carrierLogo,
        String service,
        String expected,
        BigDecimal codFee,
        BigDecimal totalFee,
        BigDecimal shippingFee
) {
}
