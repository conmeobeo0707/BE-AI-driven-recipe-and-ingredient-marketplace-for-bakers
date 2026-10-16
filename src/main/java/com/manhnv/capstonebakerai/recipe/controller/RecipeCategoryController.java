package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCategoryRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCategoryResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipe-categories")
@RequiredArgsConstructor
public class RecipeCategoryController {

    private final RecipeCategoryService recipeCategoryService;

    @Operation(summary = "Lấy danh sách categories (search + pagination + sorting)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecipeCategoryResponse>>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "name", direction = Sort.Direction.ASC)
            }) Pageable pageable) {

        PageResponse<RecipeCategoryResponse> resp = recipeCategoryService.getAll(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe category theo id")
    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<RecipeCategoryResponse>> getById(@PathVariable UUID categoryId) {
        RecipeCategoryResponse resp = recipeCategoryService.getById(categoryId);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Tạo recipe category mới", description = "Request body: name bắt buộc, không được null.")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeCategoryResponse>> create(@Valid @RequestBody RecipeCategoryRequest request) {
        RecipeCategoryResponse resp = recipeCategoryService.create(request);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Cập nhật recipe category", description = "Request body: name bắt buộc, không được null. Đây là cập nhật tên, không phải partial update.")
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<RecipeCategoryResponse>> update(@PathVariable UUID categoryId,
                                                                      @Valid @RequestBody RecipeCategoryRequest request) {
        RecipeCategoryResponse resp = recipeCategoryService.update(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Xóa recipe category")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID categoryId) {
        recipeCategoryService.delete(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}
