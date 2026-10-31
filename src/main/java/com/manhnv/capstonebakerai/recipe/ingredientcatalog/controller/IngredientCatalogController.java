package com.manhnv.capstonebakerai.recipe.ingredientcatalog.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientCatalogCandidate;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientCatalogLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredient-catalog")
@RequiredArgsConstructor
@Tag(name = "IngredientCatalogController", description = "Tra cứu nguyên liệu chuẩn đang hoạt động để người dùng chọn lại.")
public class IngredientCatalogController {

    private final IngredientCatalogLookupService lookupService;

    @GetMapping
    @Operation(summary = "Tìm nguyên liệu chuẩn", description = "CUSTOMER/ADMIN/BAKER_STAFF. keyword tùy chọn, tối đa 150 ký tự; bỏ trống để xem danh sách active. limit mặc định 10, từ 1 đến 20. Tìm tên chuẩn và alias theo EXACT, PARTIAL, FUZZY; mỗi catalog chỉ xuất hiện một lần. similarity là điểm giống tên, không phải xác suất AI nhận diện đúng. Fuzzy không tự gán nguyên liệu; người dùng chọn catalogIngredientId để confirm.")
    public ApiResponse<List<IngredientCatalogCandidate>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(lookupService.search(keyword, limit));
    }
}
