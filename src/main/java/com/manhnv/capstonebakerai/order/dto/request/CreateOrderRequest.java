package com.manhnv.capstonebakerai.order.dto.request;

import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 500) String note,
        @NotBlank @Size(max = 150) String receiverName,
        @NotBlank @Size(max = 20) String receiverPhone,
        @NotBlank String provinceCode,
        @NotBlank String districtCode,
        @NotBlank String wardCode,
        @NotBlank @Size(max = 255) String addressDetail,
        @NotBlank @Size(max = 500) String fullAddress,
        @NotBlank @Size(max = 100) String shippingRateId,
        @NotEmpty @Valid List<OrderItemRequest> items
        ) {
}
