package com.manhnv.capstonebakerai.order.mapper;

import com.manhnv.capstonebakerai.order.dto.response.OrderItemResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderSummaryResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderTrackingResponse;
import com.manhnv.capstonebakerai.order.entity.Order;
import com.manhnv.capstonebakerai.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(source = "customer.id", target = "customerId")
    OrderResponse toResponse(Order order);

    @Mapping(source = "customer.id", target = "customerId")
    OrderSummaryResponse toSummary(Order order);

    OrderTrackingResponse toTracking(Order order);
}

