package com.manhnv.capstonebakerai.user.dto;

import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Thông tin hồ sơ user hiện tại. Các trường hồ sơ chưa được nhập có thể trả về null.")
public record UserProfileResponse(
        @Schema(description = "UUID tài khoản", example = "d2719c43-6b3a-48e6-a429-43ac46f468af")
        UUID id,
        @Schema(description = "Tên đầy đủ", example = "Nguyen Van An")
        String fullName,
        @Schema(description = "Email đăng nhập", example = "an@example.com")
        String email,
        @Schema(description = "Role của tài khoản")
        Role role,
        @Schema(description = "Trạng thái tài khoản: ACTIVE, BLOCKED hoặc INACTIVE")
        UserStatus status,
        @Schema(description = "Số điện thoại; null nếu chưa khai báo", nullable = true, example = "0901234567")
        String phone,
        @Schema(description = "Địa chỉ; null nếu chưa khai báo", nullable = true, example = "Ho Chi Minh City")
        String address,
        @Schema(description = "Ngày sinh; null nếu chưa khai báo", nullable = true, example = "2002-05-21")
        String dateOfBirth,
        @Schema(description = "Giới tính; null nếu chưa khai báo", nullable = true, example = "FEMALE")
        String gender,
        @Schema(description = "URL avatar public; null nếu chưa tải avatar", nullable = true)
        String avatarUrl,
        @Schema(description = "Thời điểm tạo hồ sơ UTC")
        LocalDateTime createdAt,
        @Schema(description = "Thời điểm cập nhật hồ sơ UTC")
        LocalDateTime updatedAt
) {
}
