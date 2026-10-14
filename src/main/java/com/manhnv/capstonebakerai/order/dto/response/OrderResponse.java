package com.manhnv.capstonebakerai.order.dto.response;

import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String code,
        UUID customerId,
        UUID createdBy,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal productsPrice,
        BigDecimal shippingFee,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal total,
        BigDecimal revenue,
        String note,
        String receiverName,
        String receiverPhone,
        String provinceCode,
        String districtCode,
        String wardCode,
        String addressDetail,
        String fullAddress,
        String cancelReason,
        String carrierName,
        String shippingService,
        LocalDateTime cancelledAt,
        LocalDateTime deliveredAt,
        LocalDateTime receivedConfirmedAt,
        LocalDateTime createdAt,
        List<OrderItemResponse> items) {
}
