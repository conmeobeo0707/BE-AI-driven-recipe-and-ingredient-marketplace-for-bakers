package com.manhnv.capstonebakerai.videoextraction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecipeVideoExtractionRequest(
        @Schema(description = "ID của media VIDEO đã upload qua /api/v1/media/videos")
        @NotNull(message = "mediaId is required")
        UUID mediaId
) {
}
