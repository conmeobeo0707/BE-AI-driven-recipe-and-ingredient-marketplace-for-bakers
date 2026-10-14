package com.manhnv.capstonebakerai.order.dto.request;

import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {
}
