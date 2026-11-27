package com.manhnv.capstonebakerai.user.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.user.dto.CreateStaffAccountRequest;
import com.manhnv.capstonebakerai.user.dto.StaffAccountResponse;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.user.dto.UpdateStaffAccountRequest;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import com.manhnv.capstonebakerai.user.service.AdminAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
@Tag(name = "AdminAccountController", description = "Quản lý tài khoản STAFF, MANAGER và BAKER_STAFF. Chỉ ADMIN được gọi; cần access token Bearer.")
public class AdminAccountController {
    private final AdminAccountService adminAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo tài khoản nhân viên", description = "Tạo tài khoản không cần password từ admin. Email hướng dẫn sẽ được gửi cho nhân viên để tự đặt mật khẩu qua flow quên mật khẩu. Chỉ hỗ trợ role STAFF, MANAGER, BAKER_STAFF; phone, address, dateOfBirth, gender có thể null.")
    public ApiResponse<StaffAccountResponse> create(@Valid @RequestBody CreateStaffAccountRequest request) {
        return ApiResponse.success(adminAccountService.create(request));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tài khoản nhân viên", description = "Trả về tài khoản STAFF, MANAGER và BAKER_STAFF có phân trang, mặc định sắp xếp mới tạo trước. Có thể lọc theo status (ACTIVE/BLOCKED/INACTIVE), role (STAFF/MANAGER/BAKER_STAFF) và search (khớp một phần fullName hoặc email, không phân biệt hoa thường); bỏ trống tham số nào thì không lọc theo tiêu chí đó. page mặc định 0, size mặc định 20, tối đa 100.")
    public ApiResponse<Page<StaffAccountResponse>> getAccounts(
            @Parameter(description = "Lọc theo trạng thái tài khoản; bỏ trống để lấy tất cả") @RequestParam(required = false) UserStatus status,
            @Parameter(description = "Lọc theo vai trò; bỏ trống để lấy tất cả role được quản lý") @RequestParam(required = false) Role role,
            @Parameter(description = "Tìm theo họ tên hoặc email, khớp một phần") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(adminAccountService.getAccounts(status, role, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy tài khoản theo ID", description = "Chỉ tìm trong các role STAFF, MANAGER, BAKER_STAFF. id là UUID.")
    public ApiResponse<StaffAccountResponse> getAccount(@Parameter(description = "UUID tài khoản", required = true) @PathVariable("id") UUID id) {
        return ApiResponse.success(adminAccountService.getAccount(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin nhân viên", description = "Thay fullName, role, phone, address, dateOfBirth và gender. fullName và role bắt buộc; các field còn lại có thể null hoặc bỏ khỏi body để xóa. Chỉ hỗ trợ STAFF, MANAGER, BAKER_STAFF. Đổi role sẽ thu hồi các phiên refresh token đang hoạt động.")
    public ApiResponse<StaffAccountResponse> update(@Parameter(description = "UUID tài khoản", required = true) @PathVariable("id") UUID id, @Valid @RequestBody UpdateStaffAccountRequest request) {
        return ApiResponse.success(adminAccountService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Thay đổi trạng thái tài khoản", description = "status nhận ACTIVE, BLOCKED hoặc INACTIVE. Chuyển trạng thái sẽ thu hồi các refresh token đang hoạt động. Gửi cùng trạng thái hiện tại thì không thay đổi dữ liệu.")
    public ApiResponse<StaffAccountResponse> updateStatus(@Parameter(description = "UUID tài khoản", required = true) @PathVariable("id") UUID id, @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ApiResponse.success(adminAccountService.updateStatus(id, request));
    }
}
