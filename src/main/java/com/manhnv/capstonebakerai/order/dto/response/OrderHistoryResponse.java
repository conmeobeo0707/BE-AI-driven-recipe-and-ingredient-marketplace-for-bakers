package com.manhnv.capstonebakerai.order.dto.response;

import com.manhnv.capstonebakerai.order.enums.OrderActorType;
import com.manhnv.capstonebakerai.order.enums.OrderHistoryAction;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderHistoryResponse(
        OrderHistoryAction action,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        OrderActorType actorType,
        String note,
        LocalDateTime createdAt
) {
}
