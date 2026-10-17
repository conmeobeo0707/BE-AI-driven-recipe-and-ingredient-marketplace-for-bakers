package com.manhnv.capstonebakerai.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông báo kết quả nghiệp vụ.")
public record MessageResponse(String message) {
}
