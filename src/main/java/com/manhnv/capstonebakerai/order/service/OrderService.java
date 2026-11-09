package com.manhnv.capstonebakerai.order.service;

import com.manhnv.capstonebakerai.order.dto.request.CancelOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.CreateOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.ShippingRatesRequest;
import com.manhnv.capstonebakerai.order.dto.request.StaffCreateOrderRequest;
import com.manhnv.capstonebakerai.order.dto.response.*;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.order.enums.RevenuePeriodType;
import com.manhnv.capstonebakerai.shipment.dto.response.ShippingRateResponse;
import com.manhnv.capstonebakerai.voucher.dto.request.ValidateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherPreviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    // Customer
    OrderResponse createForCustomer(UUID userId, CreateOrderRequest request);
    Page<OrderSummaryResponse> getMyOrders(UUID userId, OrderStatus status, Pageable pageable);
    OrderResponse getMyOrder(UUID userId, UUID orderId);
    OrderTrackingResponse trackMyOrder(UUID userId, UUID orderId);
    OrderResponse cancelByCustomer(UUID userId, UUID orderId, CancelOrderRequest request);
    OrderResponse confirmReceived(UUID userId, UUID orderId);

    void syncStatusFromShipment(UUID orderId, OrderStatus next);
    void markPaidOrderPending(UUID orderId);
    List<ShippingRateResponse> previewShippingRates(ShippingRatesRequest request);
    VoucherPreviewResponse previewVoucher(UUID userId, ValidateVoucherRequest request);
    List<OrderHistoryResponse> getMyOrderHistory(UUID userId, UUID orderId);
    // Staff/Manager
    OrderResponse createByStaff(UUID staffUserId, StaffCreateOrderRequest request);
    Page<OrderSummaryResponse> getAllForStaff(OrderStatus status, PaymentMethod paymentMethod, Pageable pageable);
    OrderResponse getForStaff(UUID orderId);
    OrderResponse updateStatus(UUID orderId, OrderStatus next);
    OrderResponse cancelByStaff(UUID orderId, CancelOrderRequest request);
    RevenueReportResponse getRevenueReport(RevenuePeriodType type, LocalDate from, LocalDate to);
    List<OrderHistoryStaffResponse> getOrderHistoryForStaff(UUID orderId);
}
