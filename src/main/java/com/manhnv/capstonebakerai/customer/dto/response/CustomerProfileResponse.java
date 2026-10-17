package com.manhnv.capstonebakerai.customer.dto.response;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Phần hồ sơ riêng của Customer; các thông tin cá nhân cơ bản nằm trong UserProfileResponse.")
public record CustomerProfileResponse(
        @Schema(description = "UUID bản ghi Customer")
        UUID customerId,
        @Schema(description = "Trình độ; null nếu chưa chọn", nullable = true, example = "BEGINNER")
        SkillLevel skillLevel,
        @Schema(description = "Email đã xác minh hay chưa")
        boolean emailVerified,
        @Schema(description = "Danh sách hobby đã chọn; luôn là mảng, rỗng nếu chưa chọn")
        List<HobbyResponse> hobbies
) {
}
