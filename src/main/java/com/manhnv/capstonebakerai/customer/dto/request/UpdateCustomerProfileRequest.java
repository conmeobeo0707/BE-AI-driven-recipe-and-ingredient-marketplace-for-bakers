package com.manhnv.capstonebakerai.customer.dto.request;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cập nhật riêng skill level của Customer.")
public record UpdateCustomerProfileRequest(
        @Schema(description = "Trình độ làm bánh. Gửi null để xóa giá trị hiện tại.", example = "BEGINNER", nullable = true)
        SkillLevel skillLevel
) {
}
