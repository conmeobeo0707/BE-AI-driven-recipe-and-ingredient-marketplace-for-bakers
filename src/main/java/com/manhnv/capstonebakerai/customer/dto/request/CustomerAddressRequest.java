package com.manhnv.capstonebakerai.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerAddressRequest(
        @NotBlank @Size(max = 150) String receiverName,
        @NotBlank @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "Invalid phone number") String receiverPhone,
        @NotBlank @Size(max = 20) String provinceCode,
        @NotBlank @Size(max = 20) String districtCode,
        @NotBlank @Size(max = 20) String wardCode,
        @NotBlank @Size(max = 255) String addressDetail,
        @NotBlank @Size(max = 500) String fullAddress,
        Boolean defaultAddress
) {
}
