package com.manhnv.capstonebakerai.recipe.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeToolItemResponse {
    @Schema(description = "id dòng dụng cụ của recipe (recipe tool)")
    private UUID id;
    @Schema(description = "id dụng cụ trong danh mục")
    private UUID toolId;
    private String toolName;
    private String rawName;
    private String note;
    private BigDecimal quantity;
}
