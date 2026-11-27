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
    @Schema(nullable = true, description = "Id nguyên liệu trong danh mục. null = dòng chưa khớp danh mục (chỉ cho phép khi recipe PERSONAL, ở cấp công thức)")
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
    @Schema(example = "Sữa tươi không đường", nullable = true,
            description = "Bắt buộc khi ingredientId = null: tên hiển thị của dòng chưa khớp danh mục. Bỏ qua khi có ingredientId và ở nguyên liệu của từng bước")
    @Size(max = 255)
    private String rawName;
    @Schema(example = "a1", nullable = true,
            description = "Khoá do client đặt, duy nhất trong danh sách. Ở cấp công thức: bắt buộc khi ingredientId = null. Ở cấp bước: trỏ tới dòng công thức có cùng ref (thay cho ingredientId). Không lưu DB")
    @Size(max = 64)
    private String ref;

    public IngredientEmbeddedRequest(UUID ingredientId, BigDecimal quantity) {
        this(ingredientId, quantity, null, null, null, null, null);
    }

    public IngredientEmbeddedRequest(UUID ingredientId, BigDecimal quantity, String unit,
                                     List<SaveIngredientAliasRequest> aliases, String note) {
        this(ingredientId, quantity, unit, aliases, note, null, null);
    }
}
