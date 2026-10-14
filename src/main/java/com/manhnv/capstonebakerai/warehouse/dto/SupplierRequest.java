package com.manhnv.capstonebakerai.warehouse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        @NotBlank @Size(max = 255) String supplierName,
        @Size(max = 20) String phoneNumber,
        @Email @Size(max = 100) String email,
        @Size(max = 5000) String address) { }
