package com.manhnv.capstonebakerai.warehouse.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SupplierResponse(UUID supplierId, String supplierName, String phoneNumber,
                               String email, String address, LocalDateTime createdAt) { }
