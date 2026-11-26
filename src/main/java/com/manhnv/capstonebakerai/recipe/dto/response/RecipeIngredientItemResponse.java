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
public class RecipeIngredientItemResponse {
    @Schema(description = "id dòng nguyên liệu của recipe (recipe ingredient)")
    private UUID id;
    @Schema(description = "id nguyên liệu trong danh mục")
    private UUID ingredientId;
    private String name;
    private String rawName;
    private String note;
    private String unit;
    private BigDecimal quantity;
}
