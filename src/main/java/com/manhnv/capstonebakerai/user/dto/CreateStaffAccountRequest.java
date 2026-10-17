package com.manhnv.capstonebakerai.user.dto;

import com.manhnv.capstonebakerai.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Thông tin admin nhập để tạo tài khoản nhân viên. Không gửi password; hệ thống khởi tạo thông tin đăng nhập và gửi hướng dẫn tự đặt password qua email.")
public record CreateStaffAccountRequest(
        @Schema(description = "Tên đầy đủ, bắt buộc, tối đa 100 ký tự", example = "Nguyen Van An", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String fullName,

        @Schema(description = "Email đăng nhập duy nhất, bắt buộc", example = "an@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(description = "Role được phép tạo: STAFF, MANAGER hoặc BAKER_STAFF", example = "STAFF", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"STAFF", "MANAGER", "BAKER_STAFF"})
        @NotNull
        Role role,

        @Schema(description = "Số điện thoại; tùy chọn, null nếu chưa có", example = "0901234567", nullable = true)
        @Size(max = 20)
        String phone,

        @Schema(description = "Địa chỉ; tùy chọn, null nếu chưa có", example = "Ho Chi Minh City", nullable = true)
        @Size(max = 255)
        String address,

        @Schema(description = "Ngày sinh dạng chuỗi; tùy chọn", example = "2002-05-21", nullable = true)
        @Size(max = 10)
        String dateOfBirth,

        @Schema(description = "Giới tính dạng chuỗi; tùy chọn", example = "FEMALE", nullable = true)
        @Size(max = 20)
        String gender
) {
}
