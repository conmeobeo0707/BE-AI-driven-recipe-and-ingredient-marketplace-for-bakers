package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolEmbeddedRequest {
    @NotNull(message = "toolId is required")
    private UUID toolId;
    @NotNull(message = "quantity is required")
    private BigDecimal quantity;
    @Schema(nullable = true, description = "Ghi chú cho dụng cụ của công thức; bỏ qua ở dụng cụ của từng bước")
    private String note;

    public ToolEmbeddedRequest(UUID toolId, BigDecimal quantity) {
        this(toolId, quantity, null);
    }
}
