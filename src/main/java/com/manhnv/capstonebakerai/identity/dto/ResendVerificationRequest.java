package com.manhnv.capstonebakerai.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email
) {
}
