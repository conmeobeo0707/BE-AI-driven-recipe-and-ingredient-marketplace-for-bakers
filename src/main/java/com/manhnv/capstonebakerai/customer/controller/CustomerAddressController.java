package com.manhnv.capstonebakerai.customer.controller;


import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.dto.request.CustomerAddressRequest;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerAddressResponse;
import com.manhnv.capstonebakerai.customer.service.CustomerAddressService;
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
public class CustomerAddressController {

    private final CustomerAddressService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getMyAddresses(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> create(
            @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.create(SecurityUtils.getCurrentUserId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.update(SecurityUtils.getCurrentUserId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(SecurityUtils.getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> setDefault(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                service.setDefault(SecurityUtils.getCurrentUserId(), id)));
    }
}
