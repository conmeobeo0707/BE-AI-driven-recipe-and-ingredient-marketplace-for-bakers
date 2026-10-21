package com.manhnv.capstonebakerai.identity.dto;

import com.manhnv.capstonebakerai.user.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Thông tin access token trả về sau login/refresh. Refresh token được đặt trong HttpOnly cookie, không có trong JSON.")
public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Loại token", example = "Bearer")
        String tokenType,
        @Schema(description = "Thời hạn access token tính bằng giây", example = "3600")
        long expiresIn,
        @Schema(description = "UUID user")
        UUID userId,
        @Schema(description = "Email tài khoản")
        String email,
        @Schema(description = "Role hiện tại")
        Role role
) {
}
