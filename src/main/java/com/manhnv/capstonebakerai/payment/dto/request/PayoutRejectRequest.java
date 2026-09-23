package com.manhnv.capstonebakerai.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayoutRejectRequest {
    @NotBlank(message = "Reject reason is required")
    private String reason;
}
