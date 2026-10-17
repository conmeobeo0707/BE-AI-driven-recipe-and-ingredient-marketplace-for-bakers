package com.manhnv.capstonebakerai.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Thông tin đăng nhập.")
public record LoginRequest(
        @Schema(description = "Email tài khoản", example = "an@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,
        @Schema(description = "Mật khẩu", example = "StrongPass123", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank String password
) {
}
