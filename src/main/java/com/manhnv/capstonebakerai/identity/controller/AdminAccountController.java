package com.manhnv.capstonebakerai.identity.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.identity.dto.CreateStaffAccountRequest;
import com.manhnv.capstonebakerai.identity.dto.StaffAccountResponse;
import com.manhnv.capstonebakerai.identity.dto.UpdateAccountStatusRequest;
import com.manhnv.capstonebakerai.identity.dto.UpdateStaffAccountRequest;
import com.manhnv.capstonebakerai.identity.service.AdminAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {
    private final AdminAccountService adminAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffAccountResponse> create(@Valid @RequestBody CreateStaffAccountRequest request) {
        return ApiResponse.success(adminAccountService.create(request));
    }

    @GetMapping
    public ApiResponse<List<StaffAccountResponse>> getAccounts() {
        return ApiResponse.success(adminAccountService.getAccounts());
    }

    @GetMapping("/{id}")
    public ApiResponse<StaffAccountResponse> getAccount(@PathVariable("id") UUID id) {
        return ApiResponse.success(adminAccountService.getAccount(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<StaffAccountResponse> update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateStaffAccountRequest request) {
        return ApiResponse.success(adminAccountService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<StaffAccountResponse> updateStatus(@PathVariable("id") UUID id, @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ApiResponse.success(adminAccountService.updateStatus(id, request));
    }
}
