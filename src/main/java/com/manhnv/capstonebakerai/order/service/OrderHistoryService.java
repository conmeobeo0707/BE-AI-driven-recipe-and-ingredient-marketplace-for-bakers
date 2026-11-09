package com.manhnv.capstonebakerai.order.service;

import com.manhnv.capstonebakerai.order.dto.response.OrderHistoryResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderHistoryStaffResponse;
import com.manhnv.capstonebakerai.order.enums.OrderHistoryAction;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderHistoryService {
    void record(UUID orderId, OrderHistoryAction action, OrderStatus from, OrderStatus to, String note);

    List<OrderHistoryResponse> getForCustomer(UUID orderId);

    List<OrderHistoryStaffResponse> getForStaff(UUID orderId);
}
