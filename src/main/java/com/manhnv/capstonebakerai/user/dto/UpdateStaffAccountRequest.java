package com.manhnv.capstonebakerai.user.dto;

import com.manhnv.capstonebakerai.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Thay các thông tin hồ sơ nhân viên. Các field nullable được đặt null nếu bị bỏ khỏi body.")
public record UpdateStaffAccountRequest(
        @Schema(description = "Tên đầy đủ, bắt buộc", example = "Nguyen Van An", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String fullName,

        @Schema(description = "Role: STAFF, MANAGER hoặc BAKER_STAFF", example = "MANAGER", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"STAFF", "MANAGER", "BAKER_STAFF"})
        @NotNull
        Role role,

        @Schema(description = "Số điện thoại; null để xóa", example = "0901234567", nullable = true)
        @Size(max = 20)
        String phone,

        @Schema(description = "Địa chỉ; null để xóa", example = "Ho Chi Minh City", nullable = true)
        @Size(max = 255)
        String address,

        @Schema(description = "Ngày sinh dạng chuỗi; null để xóa", example = "2002-05-21", nullable = true)
        @Size(max = 10)
        String dateOfBirth,

        @Schema(description = "Giới tính dạng chuỗi; null để xóa", example = "FEMALE", nullable = true)
        @Size(max = 20)
        String gender
) {
}
