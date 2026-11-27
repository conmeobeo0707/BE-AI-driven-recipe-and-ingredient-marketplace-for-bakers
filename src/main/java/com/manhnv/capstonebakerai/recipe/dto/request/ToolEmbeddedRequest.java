package com.manhnv.capstonebakerai.recipe.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.math.BigDecimal;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolEmbeddedRequest {
    @Schema(nullable = true, description = "Id dụng cụ trong danh mục. null = dòng chưa khớp danh mục (chỉ cho phép khi recipe PERSONAL, ở cấp công thức)")
    private UUID toolId;
    @NotNull(message = "quantity is required")
    private BigDecimal quantity;
    @Schema(nullable = true, description = "Ghi chú cho dụng cụ của công thức; bỏ qua ở dụng cụ của từng bước")
    private String note;
    @Schema(example = "Cây lăn bột", nullable = true,
            description = "Bắt buộc khi toolId = null: tên hiển thị của dòng chưa khớp danh mục. Bỏ qua khi có toolId và ở dụng cụ của từng bước")
    @Size(max = 255)
    private String rawName;
    @Schema(example = "t1", nullable = true,
            description = "Khoá do client đặt, duy nhất trong danh sách. Ở cấp công thức: bắt buộc khi toolId = null. Ở cấp bước: trỏ tới dòng công thức có cùng ref (thay cho toolId). Không lưu DB")
    @Size(max = 64)
    private String ref;

    public ToolEmbeddedRequest(UUID toolId, BigDecimal quantity) {
        this(toolId, quantity, null, null, null);
    }

    public ToolEmbeddedRequest(UUID toolId, BigDecimal quantity, String note) {
        this(toolId, quantity, note, null, null);
    }
}
