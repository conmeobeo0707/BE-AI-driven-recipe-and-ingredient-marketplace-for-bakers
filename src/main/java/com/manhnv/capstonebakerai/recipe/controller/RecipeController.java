package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.service.RecipeService;
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
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @Operation(summary = "Lấy recipe theo id")
    @GetMapping("/{recipeId}")
    public ResponseEntity<ApiResponse<RecipeResponse>> getById(@PathVariable UUID recipeId) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.getById(recipeId)));
    }

    @Operation(summary = "Tạo recipe mới", description = "Request body: title, difficulty, prepTimeMinutes, cookTimeMinutes, servings và recipeStatus bắt buộc, không được null. description có thể null. categoryId, videoId, imageIds, tools, ingredients và steps có thể null/không gửi; khi đó không gắn category/video/ảnh và không tạo các mục con. Mỗi step trong steps phải có instruction khác null. Người tạo lấy từ tài khoản đăng nhập, không truyền trong body.")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeResponse>> create(@Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.create(request)));
    }

    @Operation(summary = "Cập nhật recipe", description = "Request body: title, difficulty, prepTimeMinutes, cookTimeMinutes, servings và recipeStatus bắt buộc, không được null. description có thể null. categoryId hoặc videoId là null để bỏ category/video. imageIds, tools, ingredients hoặc steps nếu null/không gửi sẽ xóa toàn bộ mục tương ứng hiện có; gửi danh sách rỗng cũng xóa chúng. Mỗi step trong steps phải có instruction khác null. Đây là cập nhật toàn bộ recipe, không phải partial update.")
    @PutMapping("/{recipeId}")
    public ResponseEntity<ApiResponse<RecipeResponse>> update(@PathVariable UUID recipeId,
                                                              @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.update(recipeId, request)));
    }

    @Operation(summary = "Xóa recipe")
    @DeleteMapping("/{recipeId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID recipeId) {
        recipeService.delete(recipeId);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<RecipeResponse>>> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) DifficultyEnum difficulty,
            @RequestParam(required = false) RecipeStatus recipeStatus,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        PageResponse<RecipeResponse> result = recipeService.search(
                userId,
                categoryId,
                search,
                difficulty,
                recipeStatus,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
