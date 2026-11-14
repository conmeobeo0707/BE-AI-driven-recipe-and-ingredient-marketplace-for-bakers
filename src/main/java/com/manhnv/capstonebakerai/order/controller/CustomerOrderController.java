package com.manhnv.capstonebakerai.order.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.order.dto.request.CancelOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.CreateOrderRequest;
import com.manhnv.capstonebakerai.order.dto.request.PreviewPointsRequest;
import com.manhnv.capstonebakerai.order.dto.response.*;
import com.manhnv.capstonebakerai.order.enums.OrderStatus;
import com.manhnv.capstonebakerai.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Đơn hàng của khách hàng")
public class CustomerOrderController {

    private final OrderService service;

    @Operation(summary = "API tạo đơn hàng của customer",description = "Trước đó cần gọi api Rate để lấy rateId của đơn vị vận chuyển customer chọn để tạo đơn hàng. PaymentMethod gồm COD và PAYOS")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.createForCustomer(SecurityUtils.getCurrentUserId(), request)));
    }

    @Operation(summary = "API lấy các đơn hàng của customer",description = "Có thể truyền OrderStatus để lọc theo trạng thái(cho phép null), có phân trang(mặc định page là 0, size là 20), đã sort theo createdAt")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<OrderSummaryResponse>>> myOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(
                service.getMyOrders(SecurityUtils.getCurrentUserId(), status, pageable)));
    }

    @Operation(summary = "API lấy chi tiết đơn hàng của customer")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getMyOrder(SecurityUtils.getCurrentUserId(), id)));
    }
    @Operation(summary = "API theo dõi chi tiết đơn hàng của customer")
    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> tracking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.trackMyOrder(SecurityUtils.getCurrentUserId(), id)));
    }

    @Operation(summary = "API cancel đơn hàng của customer", description = "Reason cho phép null, Customer chỉ được cancel đơn hàng khi đang ở trạng thái AWAITING_PAYMENT, PENDING, CONFIRMED")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.cancelByCustomer(SecurityUtils.getCurrentUserId(), id, request)));
    }

    @Operation(summary = "API confirmed đơn hàng của customer")
    @PatchMapping("/{id}/confirm-received")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmReceived(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.confirmReceived(SecurityUtils.getCurrentUserId(), id)));
    }

    @Operation(summary = "API xem lịch sử đơn hàng của customer", description = "Sắp xếp theo thời gian tăng dần, dùng để vẽ timeline. Không phân trang")
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<OrderHistoryResponse>>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.getMyOrderHistory(SecurityUtils.getCurrentUserId(), id)));
    }
    @Operation(
            summary = "API xem trước điểm dự kiến của đơn hàng",
            description = "Dùng ở giỏ hàng và màn đặt hàng để hiển thị \"Đơn này dự kiến mang về cho bạn X điểm\". "
                    + "Truyền danh sách items (productUnitId, quantity). voucherCode cho phép null hoặc rỗng. "
                    + "Điểm = tiền hàng sau khi trừ voucher (không gồm phí ship) / 10.000đ, làm tròn xuống. "
                    + "Đã tính nhân đôi nếu là đơn giao thành công đầu tiên và áp trần 500 điểm/đơn. "
                    + "Điểm chỉ được cộng thật khi đơn chuyển sang DELIVERED. "
                    + "Trả về expectedPoints (điểm dự kiến) và pointBase (tiền tính điểm). "
                    + "Nếu voucherCode không hợp lệ thì trả lỗi giống API kiểm tra voucher."
    )
    @PostMapping("/preview-points")
    public ResponseEntity<ApiResponse<ExpectedPointsResponse>> previewPoints(@Valid @RequestBody PreviewPointsRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.previewExpectedPoints(userId, request)));
    }
}
