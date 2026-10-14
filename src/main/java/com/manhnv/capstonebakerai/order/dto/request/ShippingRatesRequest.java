package com.manhnv.capstonebakerai.order.dto.request;

import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ShippingRatesRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotBlank String provinceCode,
        @NotBlank String districtCode,
        @NotBlank String wardCode,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
}
