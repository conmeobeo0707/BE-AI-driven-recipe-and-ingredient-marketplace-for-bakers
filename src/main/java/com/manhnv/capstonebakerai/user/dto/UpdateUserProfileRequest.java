package com.manhnv.capstonebakerai.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Các trường trong body được cập nhật cùng lúc; field tùy chọn bị bỏ hoặc gửi null sẽ được đặt thành null.")
public record UpdateUserProfileRequest(
        @Schema(description = "Tên hiển thị; bắt buộc", example = "Nguyen Van An", requiredMode = Schema.RequiredMode.REQUIRED)
        @Size(max = 100)
        String fullName,

        @Schema(description = "Số điện thoại; tùy chọn, null để xóa", example = "0901234567", nullable = true)
        @Size(max = 20)
        String phone,

        @Schema(description = "Địa chỉ; tùy chọn, null để xóa", example = "Ho Chi Minh City", nullable = true)
        @Size(max = 255)
        String address,

        @Schema(description = "Ngày sinh dạng chuỗi; tùy chọn, null để xóa", example = "2002-05-21", nullable = true)
        @Size(max = 10)
        String dateOfBirth,

        @Schema(description = "Giới tính dạng chuỗi; tùy chọn, null để xóa", example = "FEMALE", nullable = true)
        @Size(max = 20)
        String gender
) {
}
