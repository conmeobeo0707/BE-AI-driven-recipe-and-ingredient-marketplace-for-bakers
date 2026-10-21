package com.manhnv.capstonebakerai.hobby.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Thông tin một hobby.")
public record HobbyResponse(
        @Schema(description = "UUID hobby")
        UUID id,
        @Schema(description = "Tên hobby", example = "Bread baking")
        String name,
        @Schema(description = "Mô tả; null nếu không có", nullable = true)
        String description,
        @Schema(description = "Hobby có được hiển thị và cho Customer chọn không")
        boolean active
) {
}
