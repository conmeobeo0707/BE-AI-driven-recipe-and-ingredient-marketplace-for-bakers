package com.manhnv.capstonebakerai.cart.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String imgUrl,
        UUID uomId,
        String unitName,
        BigDecimal unitPrice,
        BigDecimal quantity,
        BigDecimal subtotal,
        LocalDateTime addedAt) {
}
