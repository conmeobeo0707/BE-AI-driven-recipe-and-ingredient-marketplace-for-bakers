package com.manhnv.capstonebakerai.order.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.order.dto.request.CancelOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.StaffCreateOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.UpdateOrderStatusRequest;
import com.manhnv.capstonebakerai.order.dto.response.OrderResponse;
import com.manhnv.capstonebakerai.order.dto.response.OrderSummaryResponse;
import com.manhnv.capstonebakerai.order.dto.response.RevenueReportResponse;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.enums.PaymentMethod;
import com.manhnv.capstonebakerai.order.enums.RevenuePeriodType;
import com.manhnv.capstonebakerai.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
@Tag(name = "Đơn hàng của Staff, Manager", description = "Yêu cầu quyền MANAGER, STAFF")
public class StaffOrderController {

    private final OrderService service;

    @Operation(summary = "API tạo đơn hàng cho customer, trong trường hợp khách đặt sỉ", description = "Cần truyền customerId và discountPercent")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody StaffCreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.createByStaff(SecurityUtils.getCurrentUserId(), request)));
    }

    @Operation(summary = "API lấy tất cả đơn hàng", description = "Có thể filter theo status(cho phép null -> lấy tất cả),  và paymentMethod(cho phép null -> lấy tất cả) và có phân trang(mặc định page là 0 và size là 20")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> getAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(service.getAllForStaff(status, paymentMethod, pageable)));
    }

    @Operation(summary = "API lấy doanh thu", description = "type gồm DAY, WEEK, MONTH và from, to cho phép null -> theo ngày thì FE có thể truyền null, còn theo Week hoặc month thì FE truyền tham số vào")
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> revenue(
            @RequestParam RevenuePeriodType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        return ResponseEntity.ok(ApiResponse.success(service.getRevenueReport(type, start, end)));
    }

    @Operation(summary = "Lấy chi tiết đơn hàng")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getForStaff(id)));
    }

    @Operation(summary = "Staff, Manager chỉnh sửa trạng thái đơn hàng. ", description = "Staff có thể chỉnh sửa từ PENDING -> CONFIRMED")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateStatus(id, request.status())));
    }

    @Operation(summary = "Staff, Manager cancel đơn hàng. ", description = "Staff có thể  cancel khi PENDING, CONFIRMED, READY_TO_SHIP")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.cancelByStaff(id, request)));
    }
}
