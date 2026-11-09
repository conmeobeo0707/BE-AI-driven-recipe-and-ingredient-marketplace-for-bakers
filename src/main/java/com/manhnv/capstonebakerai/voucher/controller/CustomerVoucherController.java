package com.manhnv.capstonebakerai.voucher.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.order.service.OrderService;
import com.manhnv.capstonebakerai.voucher.dto.request.ValidateVoucherRequest;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherPreviewResponse;
import com.manhnv.capstonebakerai.voucher.dto.response.VoucherSummaryResponse;
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

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
@Tag(name = "Voucher của khách hàng")
public class CustomerVoucherController {

    private final VoucherService voucherService;
    private final OrderService orderService;

    @Operation(summary = "API lấy voucher đang khả dụng (công khai)", description = "Không cần đăng nhập. Chỉ trả voucher đang bật, trong thời gian chạy và còn số lượng. Sort theo endDate tăng dần (sắp hết hạn lên trước)")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Page<VoucherSummaryResponse>>> available(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "endDate"));
        return ResponseEntity.ok(ApiResponse.success(voucherService.getAvailable(pageable)));
    }

    @Operation(summary = "API kiểm tra voucher và tính trước mức giảm", description = "Truyền voucherCode và danh sách items của giỏ hàng. Trả về số tiền được giảm để FE hiển thị khi checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<VoucherPreviewResponse>> validate(
            @Valid @RequestBody ValidateVoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.previewVoucher(SecurityUtils.getCurrentUserId(), request)));
    }
}
