package com.manhnv.capstonebakerai.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Đăng ký Customer. Role luôn do server gán CUSTOMER; avatar chưa tải khi đăng ký.")
public record RegisterUserRequest(
        @Schema(description = "Tên đầy đủ, bắt buộc, tối đa 100 ký tự", example = "Nguyen Van An", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @Schema(description = "Email hợp lệ, bắt buộc và chưa được đăng ký", example = "an@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Schema(description = "Mật khẩu ban đầu, 8 đến 72 ký tự", example = "StrongPass123", requiredMode = Schema.RequiredMode.REQUIRED, format = "password")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String password,

        @Schema(description = "Số điện thoại; tùy chọn", example = "0901234567", nullable = true)
        @Size(max = 20, message = "Phone must not exceed 20 characters")
        String phone,

        @Schema(description = "Địa chỉ; tùy chọn", example = "Ho Chi Minh City", nullable = true)
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Schema(description = "Ngày sinh dạng chuỗi; tùy chọn", example = "2002-05-21", nullable = true)
        @Size(max = 10, message = "Date of birth must not exceed 10 characters")
        String dateOfBirth,

        @Schema(description = "Giới tính dạng chuỗi; tùy chọn", example = "FEMALE", nullable = true)
        @Size(max = 20, message = "Gender must not exceed 20 characters")
        String gender
) {
}
