package com.manhnv.capstonebakerai.hobby.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateHobbyActiveRequest(
        @Schema(description = "true để bật, false để tắt hobby", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean active
) {
}
