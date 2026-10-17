package com.manhnv.capstonebakerai.user.dto;

import com.manhnv.capstonebakerai.user.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @Schema(description = "Trạng thái tài khoản", example = "ACTIVE", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"ACTIVE", "BLOCKED", "INACTIVE"})
        @NotNull
        UserStatus status
) {
}
