package com.manhnv.capstonebakerai.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PreviewPointsRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        String voucherCode
        ) {
}
