package com.manhnv.capstonebakerai.expense.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.expense.dto.request.CancelExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.request.CreateExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.request.UpdateExpenseVoucherRequest;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseReportResponse;
import com.manhnv.capstonebakerai.expense.dto.response.ExpenseVoucherResponse;
import com.manhnv.capstonebakerai.expense.service.ExpenseVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expense-vouchers")
@RequiredArgsConstructor
@Tag(name = "Phiếu chi", description = "Các API quản lý phiếu chi, yêu cầu quyền MANAGER, STAFF")
public class ExpenseVoucherController {

    private final ExpenseVoucherService service;

    @Operation(summary = "API tạo phiếu chi", description = "Yêu cầu expenseCategoryId và amount(số tiền) và reason cho phép null")
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ExpenseVoucherResponse>> create(@Valid @RequestBody CreateExpenseVoucherRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(service.create(userId, request)));
    }

    @Operation(summary = "API cập nhật phiếu chi", description = "Yêu cầu expenseCategoryId và amount(số tiền) và reason cho phép null")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ExpenseVoucherResponse>> update(@PathVariable UUID id, @Valid @RequestBody UpdateExpenseVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @Operation(summary = "API hủy phiếu chi", description = "cancelReason được phép null")
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ExpenseVoucherResponse>> cancel(@PathVariable UUID id, @Valid @RequestBody(required = false)CancelExpenseVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.cancel(id, request)));
    }

    @Operation(summary = "API lấy chi tiết phiếu chi")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ExpenseVoucherResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @Operation(summary = "API lấy danh sách phiếu chi, có phân trang")
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<ExpenseVoucherResponse>>> getAll(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(pageable)));
    }

    @Operation(summary = "API lấy báo cáo phiếu chi theo tháng", description = "month cho phép null(nếu null thì hệ thống sẽ tính month từ tháng đó, categoryId cho phép null(hệ thống sẽ lấy toàn bộ category). Đây là báo cáo tổng loại chi này bao nhiêu phiếu chi, bao nhiêu tiền.")
    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<ExpenseReportResponse>> getReport(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")YearMonth month,
            @RequestParam(required = false) UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.success(service.getReport(month, categoryId)));
    }

    @Operation(summary = "API lấy chi tiết phiếu chi", description = "month cho phép null(nếu null thì hệ thống sẽ tính month từ tháng đó, categoryId cho phép null(hệ thống sẽ lấy toàn bộ category). Đây là báo cáo chi tiết của phiếu chi.")
    @GetMapping("/report-detail")
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<ExpenseVoucherResponse>>> listVoucherFromReport(
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(ApiResponse.success(service.getListVoucherFromReport(month, categoryId, pageable)));
    }
}
