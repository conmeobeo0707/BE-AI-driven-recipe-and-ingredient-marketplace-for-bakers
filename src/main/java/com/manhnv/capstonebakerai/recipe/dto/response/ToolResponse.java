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
    private UUID recipeId;
    private String name;
    private BigDecimal quantity;
}
