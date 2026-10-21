package com.manhnv.capstonebakerai.hobby.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dữ liệu tạo hoặc thay thế thông tin hobby.")
public record SaveHobbyRequest(
        @Schema(description = "Tên hobby, bắt buộc, tối đa 100 ký tự và không được trùng", example = "Bread baking", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String name,

        @Schema(description = "Mô tả tùy chọn; null hoặc chuỗi trắng được lưu thành null", example = "Learn to bake sourdough bread", nullable = true)
        @Size(max = 255)
        String description
) {
}
