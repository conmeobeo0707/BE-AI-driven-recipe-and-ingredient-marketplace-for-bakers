package com.manhnv.capstonebakerai.customer.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerProfileResponse;
import com.manhnv.capstonebakerai.customer.dto.request.UpdateCustomerHobbiesRequest;
import com.manhnv.capstonebakerai.customer.dto.request.UpdateCustomerProfileRequest;
import com.manhnv.capstonebakerai.customer.service.CustomerProfileService;
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
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    public ApiResponse<CustomerProfileResponse> getProfile() {
        return ApiResponse.success(customerProfileService.getProfile(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping
    public ApiResponse<CustomerProfileResponse> updateProfile(
            @Valid @RequestBody UpdateCustomerProfileRequest request
    ) {
        return ApiResponse.success(customerProfileService.updateProfile(
                SecurityUtils.getCurrentUserId(), request));
    }

    @PutMapping("/hobbies")
    public ApiResponse<CustomerProfileResponse> updateHobbies(
            @Valid @RequestBody UpdateCustomerHobbiesRequest request
    ) {
        return ApiResponse.success(customerProfileService.updateHobbies(
                SecurityUtils.getCurrentUserId(), request));
    }
}
