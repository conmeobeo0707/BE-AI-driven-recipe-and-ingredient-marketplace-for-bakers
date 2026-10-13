package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeBookmarkRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeBookmarkResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeBookmarkService;
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
@RequestMapping("/api/v1/recipe-bookmarks")
@RequiredArgsConstructor
public class RecipeBookmarkController {

    private final RecipeBookmarkService recipeBookmarkService;

    @Operation(summary = "Lấy danh sách recipe bookmarks (search + pagination + sorting)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecipeBookmarkResponse>>> getAll(
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        PageResponse<RecipeBookmarkResponse> resp = recipeBookmarkService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe bookmark theo id")
    @GetMapping("/{recipeBookmarkId}")
    public ResponseEntity<ApiResponse<RecipeBookmarkResponse>> getById(@PathVariable UUID recipeBookmarkId) {
        return ResponseEntity.ok(ApiResponse.success(recipeBookmarkService.getById(recipeBookmarkId)));
    }

    @Operation(summary = "Lấy recipe bookmarks theo customerId (search + pagination + sorting)")
    @GetMapping("/user/{customerId}")
    public ResponseEntity<ApiResponse<PageResponse<RecipeBookmarkResponse>>> getByUser(
            @PathVariable UUID customerId,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        PageResponse<RecipeBookmarkResponse> resp = recipeBookmarkService.getByCustomerId(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe bookmarks theo recipeId (search + pagination + sorting)")
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<PageResponse<RecipeBookmarkResponse>>> getByRecipe(
            @PathVariable UUID recipeId,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {


        PageResponse<RecipeBookmarkResponse> resp = recipeBookmarkService.getByRecipeId(recipeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Tạo recipe bookmark mới")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeBookmarkResponse>> create(@Valid @RequestBody RecipeBookmarkRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeBookmarkService.create(request)));
    }

    @Operation(summary = "Cập nhật recipe bookmark")
    @PutMapping("/{recipeBookmarkId}")
    public ResponseEntity<ApiResponse<RecipeBookmarkResponse>> update(@PathVariable UUID recipeBookmarkId,
                                                                      @Valid @RequestBody RecipeBookmarkRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeBookmarkService.update(recipeBookmarkId, request)));
    }

    @Operation(summary = "Xóa recipe bookmark")
    @DeleteMapping("/{recipeBookmarkId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID recipeBookmarkId) {
        recipeBookmarkService.delete(recipeBookmarkId);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}