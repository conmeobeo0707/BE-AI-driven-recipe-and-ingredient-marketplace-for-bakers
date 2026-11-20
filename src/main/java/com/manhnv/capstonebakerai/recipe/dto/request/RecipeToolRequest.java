package com.manhnv.capstonebakerai.recipe.dto.request;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import java.math.BigDecimal;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RecipeToolRequest {
 @NotNull private UUID recipeId;
 @NotNull private UUID toolId;
 @NotNull private BigDecimal quantity;
}
