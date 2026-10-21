package com.manhnv.capstonebakerai.recipe.dto.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolRequest {
    @NotNull(message = "recipeId is required")
    private UUID recipeId;
    @NotNull(message = "name is required")
    private String name;
}
