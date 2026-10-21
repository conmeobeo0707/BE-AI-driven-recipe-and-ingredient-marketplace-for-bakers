package com.manhnv.capstonebakerai.customer.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerProfileResponse;
import com.manhnv.capstonebakerai.customer.dto.request.UpdateCustomerHobbiesRequest;
import com.manhnv.capstonebakerai.customer.dto.request.UpdateCustomerProfileRequest;
import com.manhnv.capstonebakerai.customer.service.CustomerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/me")
@RequiredArgsConstructor
@Tag(name = "CustomerProfileController", description = "Hồ sơ bổ sung của Customer. Chỉ role CUSTOMER; cần access token Bearer.")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    @Operation(summary = "Lấy hồ sơ Customer", description = "Trả về skillLevel, trạng thái emailVerified và hobbies hiện tại. skillLevel có thể null; hobbies luôn là danh sách, có thể rỗng.")
    public ApiResponse<CustomerProfileResponse> getProfile() {
        return ApiResponse.success(customerProfileService.getProfile(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping
    @Operation(summary = "Cập nhật trình độ làm bánh", description = "Body gồm skillLevel: BEGINNER, INTERMEDIATE hoặc ADVANCED. Có thể truyền null để xóa skill level. Đây là cập nhật trường skill level, không thay đổi sở thích.")
    public ApiResponse<CustomerProfileResponse> updateProfile(
            @Valid @RequestBody UpdateCustomerProfileRequest request
    ) {
        return ApiResponse.success(customerProfileService.updateProfile(
                SecurityUtils.getCurrentUserId(), request));
    }

    @PutMapping("/hobbies")
    @Operation(summary = "Thay toàn bộ danh sách sở thích", description = "hobbyIds là tập UUID của hobby đang active. Gửi [] để xóa tất cả sở thích. Mọi ID phải tồn tại và đang active; nếu một ID không hợp lệ, request bị từ chối. Hobbies hiện tại được thay hoàn toàn bằng danh sách mới.")
    public ApiResponse<CustomerProfileResponse> updateHobbies(
            @Valid @RequestBody UpdateCustomerHobbiesRequest request
    ) {
        return ApiResponse.success(customerProfileService.updateHobbies(
                SecurityUtils.getCurrentUserId(), request));
    }
}
