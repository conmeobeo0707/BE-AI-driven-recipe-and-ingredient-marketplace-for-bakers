package com.manhnv.capstonebakerai.customer.dto.response;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Thông tin Customer và hồ sơ User công khai; không bao gồm password.")
public record AdminCustomerResponse(
        @Schema(description = "UUID bản ghi Customer")
        UUID customerId,
        @Schema(description = "UUID tài khoản User")
        UUID userId,
        @Schema(description = "Tên đầy đủ")
        String fullName,
        @Schema(description = "Email đăng nhập")
        String email,
        @Schema(description = "Trạng thái tài khoản: ACTIVE, BLOCKED hoặc INACTIVE")
        UserStatus status,
        @Schema(description = "Số điện thoại; null nếu chưa khai báo", nullable = true)
        String phone,
        @Schema(description = "Địa chỉ; null nếu chưa khai báo", nullable = true)
        String address,
        @Schema(description = "Ngày sinh; null nếu chưa khai báo", nullable = true)
        String dateOfBirth,
        @Schema(description = "Giới tính; null nếu chưa khai báo", nullable = true)
        String gender,
        @Schema(description = "URL avatar; null nếu chưa tải ảnh", nullable = true)
        String avatarUrl,
        @Schema(description = "Trình độ; null nếu chưa chọn", nullable = true)
        SkillLevel skillLevel,
        @Schema(description = "Email đã xác minh hay chưa")
        boolean emailVerified,
        @Schema(description = "Thời điểm tạo User UTC")
        LocalDateTime createdAt,
        @Schema(description = "Thời điểm cập nhật hồ sơ User UTC")
        LocalDateTime updatedAt
) {
}
