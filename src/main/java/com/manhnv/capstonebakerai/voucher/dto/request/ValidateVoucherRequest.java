package com.manhnv.capstonebakerai.voucher.dto.request;

import com.manhnv.capstonebakerai.order.dto.request.OrderItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ValidateVoucherRequest(
        @NotBlank @Size(max = 50) String voucherCode,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
}
