package com.manhnv.capstonebakerai.user.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.user.dto.CreateStaffAccountRequest;
import com.manhnv.capstonebakerai.user.dto.StaffAccountResponse;
import com.manhnv.capstonebakerai.user.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.user.dto.UpdateStaffAccountRequest;
import com.manhnv.capstonebakerai.user.service.AdminAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    @Operation(summary = "Lấy danh sách tài khoản nhân viên", description = "Trả về toàn bộ tài khoản STAFF, MANAGER và BAKER_STAFF, mới tạo trước. API hiện chưa phân trang.")
    public ApiResponse<List<StaffAccountResponse>> getAccounts() {
        return ApiResponse.success(adminAccountService.getAccounts());
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
