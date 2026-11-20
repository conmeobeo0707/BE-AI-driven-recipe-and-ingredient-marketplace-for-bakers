package com.manhnv.capstonebakerai.order.dto.response;

import com.manhnv.capstonebakerai.order.enums.OrderActorType;
import com.manhnv.capstonebakerai.order.enums.OrderHistoryAction;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderHistoryStaffResponse(
        UUID id,
        OrderHistoryAction action,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        OrderActorType actorType,
        UUID actorId,
        String note,
        LocalDateTime createdAt
) {
}
