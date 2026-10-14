package com.manhnv.capstonebakerai.customer.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.customer.dto.response.CustomerAddressResponse;
import com.manhnv.capstonebakerai.customer.service.CustomerAddressService;
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
public class StaffCustomerAddressController {

    private final CustomerAddressService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> getByCustomer(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getByCustomerId(customerId)));
    }
}
