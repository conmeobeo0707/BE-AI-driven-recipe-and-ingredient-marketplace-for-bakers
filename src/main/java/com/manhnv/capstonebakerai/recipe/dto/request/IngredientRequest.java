package com.manhnv.capstonebakerai.recipe.dto.request;

import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.SaveIngredientAliasRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;


@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientRequest {
    @NotNull(message = "name is required")
    private String name;
    @Schema(description = "Tên gọi khác, chỉ dùng khi tạo mới; sửa alias qua /api/v1/ingredients/{id}/aliases")
    @Valid
    @Size(max = 20)
    private List<SaveIngredientAliasRequest> aliases;
}
