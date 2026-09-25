package com.manhnv.capstonebakerai.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must contain exactly 6 digits") String code,
        @NotBlank @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters") String newPassword
) {
}
