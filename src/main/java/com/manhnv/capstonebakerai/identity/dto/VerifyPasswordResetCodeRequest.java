package com.manhnv.capstonebakerai.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPasswordResetCodeRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must contain exactly 6 digits") String code
) {
}
