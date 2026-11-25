package com.manhnv.capstonebakerai.recipe.dto.request;

import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.SaveIngredientAliasRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientEmbeddedRequest {
    @NotNull(message = "ingredientId is required")
    private UUID ingredientId;
    @Schema(example = "2")
    @NotNull(message = "quantity is required")
    private BigDecimal quantity;
    @Schema(example = "g", description = "Bắt buộc ở nguyên liệu của công thức; bỏ qua ở nguyên liệu của từng bước")
    @Size(max = 255)
    private String unit;
    @Schema(description = "Tên gọi khác thêm vào nguyên liệu này khi lưu công thức; bỏ qua ở nguyên liệu của từng bước")
    @Valid
    @Size(max = 20)
    private List<SaveIngredientAliasRequest> aliases;
    @Schema(example = "đập nhuyễn", nullable = true, description = "Ghi chú cho nguyên liệu của công thức; bỏ qua ở nguyên liệu của từng bước")
    private String note;

    public IngredientEmbeddedRequest(UUID ingredientId, BigDecimal quantity) {
        this(ingredientId, quantity, null, null, null);
    }
}
