package com.manhnv.capstonebakerai.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Kiểm tra mã đặt lại mật khẩu trước khi người dùng nhập mật khẩu mới.")
public record VerifyPasswordResetCodeRequest(
        @Schema(description = "Email tài khoản", example = "an@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,
        @Schema(description = "Mã 6 chữ số", example = "012345", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must contain exactly 6 digits") String code
) {
}
