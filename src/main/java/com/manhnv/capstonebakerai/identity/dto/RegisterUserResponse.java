package com.manhnv.capstonebakerai.identity.dto;

import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Hồ sơ tài khoản Customer vừa đăng ký. Mật khẩu không được trả về.")
public record RegisterUserResponse(
        @Schema(description = "UUID tài khoản")
        UUID id,
        @Schema(description = "Tên đầy đủ")
        String fullName,
        @Schema(description = "Email đăng nhập")
        String email,
        @Schema(description = "Role do server gán; đăng ký công khai luôn là CUSTOMER")
        Role role,
        @Schema(description = "Số điện thoại; null nếu chưa cung cấp", nullable = true)
        String phone,
        @Schema(description = "Trạng thái tài khoản")
        UserStatus status,
        @Schema(description = "Địa chỉ; null nếu chưa cung cấp", nullable = true)
        String address,
        @Schema(description = "Ngày sinh; null nếu chưa cung cấp", nullable = true)
        String dateOfBirth,
        @Schema(description = "Giới tính; null nếu chưa cung cấp", nullable = true)
        String gender,
        @Schema(description = "Thời điểm tạo UTC")
        LocalDateTime createdAt
) {
}
