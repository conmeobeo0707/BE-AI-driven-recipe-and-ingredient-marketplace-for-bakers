package com.manhnv.capstonebakerai.customer.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.customer.dto.request.AdminUpdateCustomerRequest;
import com.manhnv.capstonebakerai.customer.dto.response.AdminCustomerResponse;
import com.manhnv.capstonebakerai.customer.service.AdminCustomerService;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@Tag(name = "AdminCustomerController", description = "Quản lý danh sách Customer. Chỉ ADMIN được gọi; cần access token Bearer.")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping
    @Operation(summary = "Lấy danh sách Customer", description = "Trả về Customer có phân trang cùng các thông tin User, skill level và trạng thái xác minh email, mặc định sắp xếp mới tạo trước. Có thể lọc theo status (ACTIVE/BLOCKED/INACTIVE) và search (khớp một phần fullName hoặc email, không phân biệt hoa thường); bỏ trống tham số nào thì không lọc theo tiêu chí đó. page mặc định 0, size mặc định 20, tối đa 100. Các trường hồ sơ tùy chọn có thể null; không trả về password.")
    public ApiResponse<Page<AdminCustomerResponse>> getCustomers(
            @Parameter(description = "Lọc theo trạng thái tài khoản; bỏ trống để lấy tất cả") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "Tìm theo họ tên hoặc email, khớp một phần") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "user.createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(adminCustomerService.getCustomers(status, search, pageable));
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Cập nhật hồ sơ Customer", description = "Thay fullName, phone, address, dateOfBirth và gender. fullName bắt buộc; các field còn lại có thể null hoặc bỏ khỏi body để xóa. customerId là UUID bản ghi Customer (không phải userId).")
    public ApiResponse<AdminCustomerResponse> update(
            @Parameter(description = "UUID bản ghi Customer", required = true) @PathVariable("customerId") UUID customerId,
            @Valid @RequestBody AdminUpdateCustomerRequest request
    ) {
        return ApiResponse.success(adminCustomerService.updateCustomer(customerId, request));
    }

    @PatchMapping("/{customerId}/status")
    @Operation(summary = "Thay đổi trạng thái tài khoản Customer", description = "status nhận ACTIVE, BLOCKED hoặc INACTIVE. Chuyển trạng thái sẽ thu hồi các refresh token đang hoạt động. Gửi cùng trạng thái hiện tại thì không thay đổi dữ liệu.")
    public ApiResponse<AdminCustomerResponse> updateStatus(
            @Parameter(description = "UUID bản ghi Customer", required = true) @PathVariable("customerId") UUID customerId,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        return ApiResponse.success(adminCustomerService.updateCustomerStatus(customerId, request));
    }
}
