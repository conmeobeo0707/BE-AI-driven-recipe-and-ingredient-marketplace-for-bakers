package com.manhnv.capstonebakerai.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        UUID productUnitId,
        String productName,
        String sku,
        String unitName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
