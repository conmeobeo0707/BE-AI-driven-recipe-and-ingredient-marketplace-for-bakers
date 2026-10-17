package com.manhnv.capstonebakerai.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Thông tin xác minh email.")
public record VerifyEmailRequest(
        @Schema(description = "Email đã đăng ký", example = "an@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,

        @Schema(description = "Mã xác minh đúng 6 chữ số", example = "012345", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{6}", message = "Verification code must contain exactly 6 digits")
        String code
) {
}
