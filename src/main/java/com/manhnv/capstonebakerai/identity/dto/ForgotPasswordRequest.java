package com.manhnv.capstonebakerai.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu mã đặt lại mật khẩu.")
public record ForgotPasswordRequest(
        @Schema(description = "Email tài khoản cần đặt lại mật khẩu", example = "an@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email
) {
}
