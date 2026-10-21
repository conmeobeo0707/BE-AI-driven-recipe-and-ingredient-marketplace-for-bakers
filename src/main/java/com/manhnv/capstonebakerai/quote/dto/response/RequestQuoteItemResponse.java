package com.manhnv.capstonebakerai.quote.dto.response;

import java.util.UUID;

public record RequestQuoteItemResponse(
        UUID productUnitId,
        UUID productId,
        String productName,
        String sku,
        String unitName,
        int quantity
) {
}
