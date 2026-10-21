package com.manhnv.capstonebakerai.customer.controller;


import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.dto.request.CustomerAddressRequest;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerAddressResponse;
import com.manhnv.capstonebakerai.customer.service.CustomerAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/me/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Địa chỉ của Customer")
public class CustomerAddressController {

    private final CustomerAddressService service;

    @Operation(summary = "API lấy danh sách địa chỉ của customer để customer chọn khi order, trong đó có defaultAddress là kiểu boolean là địa chỉ mặc định")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getMyAddresses(SecurityUtils.getCurrentUserId())));
    }

    @Operation(summary = "API tạo địa chỉ cho customer khi customer order và không muốn dùng những địa chỉ kia thì sẽ tạo thêm địa chỉ mới")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> create(
            @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.create(SecurityUtils.getCurrentUserId(), request)));
    }

    @Operation(summary = "API update địa chỉ cho customer")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.update(SecurityUtils.getCurrentUserId(), id, request)));
    }

    @Operation(summary = "API xóa địa chỉ cho customer")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "API đặt địa chỉ làm mặc định cho customer")
    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> setDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.setDefault(SecurityUtils.getCurrentUserId(), id)));
    }
}
