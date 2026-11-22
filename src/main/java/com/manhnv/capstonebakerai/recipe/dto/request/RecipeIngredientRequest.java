package com.manhnv.capstonebakerai.recipe.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RecipeIngredientRequest {
 @NotNull private UUID recipeId;
 @NotNull private UUID ingredientId;
 @NotNull private BigDecimal quantity;
 @NotBlank @Size(max = 255) private String unit;
}
