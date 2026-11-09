package com.manhnv.capstonebakerai.customer.dto.response;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Thông tin hiển thị của Customer để dùng làm tác giả, ví dụ trong comment.")
public record CustomerPublicProfileResponse(
        @Schema(description = "UUID tài khoản User; khớp với userId trong comment")
        UUID userId,
        @Schema(description = "UUID hồ sơ Customer")
        UUID customerId,
        @Schema(description = "Tên hiển thị của Customer")
        String fullName,
        @Schema(description = "URL avatar; null nếu Customer chưa tải avatar", nullable = true)
        String avatarUrl,
        @Schema(description = "Trình độ làm bánh; null nếu chưa chọn", nullable = true)
        SkillLevel skillLevel,
        @Schema(description = "Giới tính; null nếu chưa khai báo", nullable = true)
        String gender,
        @Schema(description = "Ngày sinh; null nếu chưa khai báo", nullable = true, example = "2002-05-21")
        String dateOfBirth,
        @Schema(description = "Các hobby Customer đã chọn; luôn là danh sách, rỗng nếu chưa chọn")
        List<HobbyResponse> hobbies
) {
}
