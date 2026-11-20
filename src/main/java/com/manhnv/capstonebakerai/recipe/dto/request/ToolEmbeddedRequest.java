package com.manhnv.capstonebakerai.recipe.dto.request;

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
}
