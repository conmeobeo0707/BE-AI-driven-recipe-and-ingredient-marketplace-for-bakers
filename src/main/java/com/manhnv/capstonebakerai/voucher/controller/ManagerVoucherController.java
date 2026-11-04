package com.manhnv.capstonebakerai.voucher.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.voucher.dto.request.CreateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.request.UpdateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.request.UpdateVoucherStatusRequest;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherUsageResponse;
import com.manhnv.capstonebakerai.voucher.enums.VoucherDisplayStatus;
import com.manhnv.capstonebakerai.voucher.service.VoucherService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/manager/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
@Tag(name = "Quản lý voucher (Manager)")
public class ManagerVoucherController {

    private final VoucherService service;

    @Operation(summary = "API tạo voucher", description = "discountType gồm PERCENT và FIXED_AMOUNT. maxDiscountAmount chỉ áp dụng cho PERCENT. Code cho phép null/để trống: hệ thống sẽ tự sinh. Nếu nhập thì không phân biệt hoa thường, hệ thống lưu in hoa")
    @PostMapping
    public ResponseEntity<ApiResponse<VoucherResponse>> create(@Valid @RequestBody CreateVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.create(SecurityUtils.getCurrentUserId(), request)));
    }

    @Operation(summary = "API cập nhật voucher", description = "Nếu voucher đã có người dùng thì chỉ được sửa name, description, endDate, startDate, tăng totalQuantity")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @Operation(summary = "API bật/tắt voucher", description = "status gồm ACTIVE và INACTIVE. Đơn đã áp voucher trước đó không bị ảnh hưởng")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<VoucherResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVoucherStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updateStatus(id, request.status())));
    }

    @Operation(summary = "API lấy danh sách voucher", description = "Lọc theo displayStatus (UPCOMING, RUNNING, EXPIRED, OUT_OF_STOCK, DISABLED) và keyword (code hoặc name), cho phép null. Có phân trang (mặc định page 0, size 20), sort theo createdAt giảm dần")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VoucherResponse>>> getAll(
            @RequestParam(required = false) VoucherDisplayStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(service.getAll(status, keyword, pageable)));
    }

    @Operation(summary = "API lấy chi tiết voucher")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @Operation(summary = "API xem lịch sử sử dụng voucher", description = "Có phân trang, sort theo usedAt giảm dần")
    @GetMapping("/{id}/usages")
    public ResponseEntity<ApiResponse<Page<VoucherUsageResponse>>> usages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "usedAt"));
        return ResponseEntity.ok(ApiResponse.success(service.getUsages(id, pageable)));
    }
}
