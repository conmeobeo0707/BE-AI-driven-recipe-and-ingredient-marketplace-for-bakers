package com.manhnv.capstonebakerai.order.dto.response;

public record OrderStatusSummaryResponse(
        long totalOrders,
        long pendingApproval,
        long shipping,
        long completed
) {}
