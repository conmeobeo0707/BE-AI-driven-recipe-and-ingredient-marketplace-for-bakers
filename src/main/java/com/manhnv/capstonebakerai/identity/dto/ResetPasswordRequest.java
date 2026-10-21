package com.manhnv.capstonebakerai.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Đặt mật khẩu mới bằng email và mã đặt lại còn hiệu lực. Mã vẫn cần gửi lại dù đã gọi API verify.")
public record ResetPasswordRequest(
        @Schema(description = "Email tài khoản", example = "an@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,
        @Schema(description = "Mã đặt lại mật khẩu 6 chữ số", example = "012345", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must contain exactly 6 digits") String code,
        @Schema(description = "Mật khẩu mới, 8 đến 72 ký tự", example = "NewStrongPass123", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters") String newPassword
) {
}
