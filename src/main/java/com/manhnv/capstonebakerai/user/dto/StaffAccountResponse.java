package com.manhnv.capstonebakerai.user.dto;

import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Thông tin tài khoản nhân viên; các trường hồ sơ tùy chọn có thể null.")
public record StaffAccountResponse(
        @Schema(description = "UUID tài khoản")
        UUID id,
        @Schema(description = "Tên đầy đủ")
        String fullName,
        @Schema(description = "Email đăng nhập")
        String email,
        @Schema(description = "Role tài khoản")
        Role role,
        @Schema(description = "Trạng thái tài khoản")
        UserStatus status,
        @Schema(description = "Số điện thoại; null nếu chưa khai báo", nullable = true)
        String phone,
        @Schema(description = "Địa chỉ; null nếu chưa khai báo", nullable = true)
        String address,
        @Schema(description = "Ngày sinh; null nếu chưa khai báo", nullable = true)
        String dateOfBirth,
        @Schema(description = "Giới tính; null nếu chưa khai báo", nullable = true)
        String gender,
        @Schema(description = "URL avatar; null nếu chưa tải avatar", nullable = true)
        String avatarUrl,
        @Schema(description = "Thời điểm tạo UTC")
        LocalDateTime createdAt,
        @Schema(description = "Thời điểm cập nhật gần nhất UTC")
        LocalDateTime updatedAt
) {
}
