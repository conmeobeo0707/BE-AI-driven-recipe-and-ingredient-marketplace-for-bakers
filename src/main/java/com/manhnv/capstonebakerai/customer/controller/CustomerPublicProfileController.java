package com.manhnv.capstonebakerai.customer.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerPublicProfileResponse;
import com.manhnv.capstonebakerai.customer.service.CustomerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "CustomerPublicProfileController", description = "Thông tin hiển thị công khai của Customer; cần access token Bearer.")
public class CustomerPublicProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/{userId}")
    @Operation(
            summary = "Lấy thông tin hiển thị của Customer theo User ID",
            description = "Dùng userId từ comment để lấy tên, avatar, skill level, giới tính, ngày sinh và các hobby kèm thông tin chi tiết. Không trả email, số điện thoại hoặc địa chỉ."
    )
    public ApiResponse<CustomerPublicProfileResponse> getByUserId(
            @PathVariable UUID userId
    ) {
        return ApiResponse.success(customerProfileService.getPublicProfileByUserId(userId));
    }
}
