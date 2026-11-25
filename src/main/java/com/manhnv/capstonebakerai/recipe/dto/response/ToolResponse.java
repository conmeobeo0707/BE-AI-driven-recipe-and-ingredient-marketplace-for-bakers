package com.manhnv.capstonebakerai.recipe.dto.response;

import lombok.*;

import java.util.UUID;
import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResponse {
    private UUID id;
    private UUID recipeToolId;
    private UUID recipeId;
    private String name;
    private String rawName;
    private String note;
    private BigDecimal quantity;
}
