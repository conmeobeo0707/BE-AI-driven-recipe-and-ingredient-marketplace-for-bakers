package com.manhnv.capstonebakerai.recipe.dto.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolEmbeddedRequest {
    @NotNull(message = "name is required")
    private String name;
}
