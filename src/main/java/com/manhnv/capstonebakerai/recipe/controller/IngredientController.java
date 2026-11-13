package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.IngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.IngredientResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @Operation(summary = "Lấy tất cả ingredients")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<IngredientResponse>>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "name", direction = Sort.Direction.ASC)
            }) Pageable pageable) {

        PageResponse<IngredientResponse> resp = ingredientService.getAll(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy ingredient theo id")
    @GetMapping("/{ingredientId}")
    public ResponseEntity<ApiResponse<IngredientResponse>> getById(@PathVariable UUID ingredientId) {
        return ResponseEntity.ok(ApiResponse.success(ingredientService.getById(ingredientId)));
    }

    @Operation(summary = "Tạo ingredient mới", description = "Request body: name và unit đều bắt buộc.")
    @PostMapping
    public ResponseEntity<ApiResponse<IngredientResponse>> create(@Valid @RequestBody IngredientRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ingredientService.create(request)));
    }

    @Operation(summary = "Cập nhật ingredient", description = "Request body: name và unit đều bắt buộc. Đây là cập nhật toàn bộ, không phải partial update.")
    @PutMapping("/{ingredientId}")
    public ResponseEntity<ApiResponse<IngredientResponse>> update(@PathVariable UUID ingredientId,
                                                                  @Valid @RequestBody IngredientRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ingredientService.update(ingredientId, request)));
    }

    @Operation(summary = "Xóa ingredient")
    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID ingredientId) {
        ingredientService.delete(ingredientId);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}
