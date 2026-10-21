package com.manhnv.capstonebakerai.customer.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.customer.dto.response.AdminCustomerResponse;
import com.manhnv.capstonebakerai.customer.service.AdminCustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@Tag(name = "AdminCustomerController", description = "Quản lý danh sách Customer. Chỉ ADMIN được gọi; cần access token Bearer.")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping
    @Operation(summary = "Lấy danh sách Customer", description = "Trả về Customer mới tạo trước cùng các thông tin User, skill level và trạng thái xác minh email. Các trường hồ sơ tùy chọn có thể null; không trả về password. API hiện chưa phân trang.")
    public ApiResponse<List<AdminCustomerResponse>> getCustomers() {
        return ApiResponse.success(adminCustomerService.getCustomers());
    }
}
