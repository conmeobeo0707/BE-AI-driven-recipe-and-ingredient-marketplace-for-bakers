package com.manhnv.capstonebakerai.customer.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerAddressResponse(
        UUID id,
        String receiverName,
        String receiverPhone,
        String provinceCode,
        String districtCode,
        String wardCode,
        String addressDetail,
        String fullAddress,
        boolean defaultAddress,
        LocalDateTime createdAt
) {
}
