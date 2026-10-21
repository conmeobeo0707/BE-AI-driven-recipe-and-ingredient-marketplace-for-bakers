package com.manhnv.capstonebakerai.customer.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerAddressResponse;
import com.manhnv.capstonebakerai.customer.service.CustomerAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/staff/customers/{customerId}/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
@Tag(name = "Địa chỉ customer cho staff")
public class StaffCustomerAddressController {

    private final CustomerAddressService service;

    @Operation(summary = "API lấy danh sách địa chỉ của customer khi staff tạo đơn cho customer để staff chọn và tạo order")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getByCustomer(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getByCustomerId(customerId)));
    }
}
