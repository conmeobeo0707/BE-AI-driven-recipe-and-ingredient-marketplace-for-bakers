package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeCommentRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeCommentResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeCommentService;
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
@RequestMapping("/api/v1/recipe-comments")
@RequiredArgsConstructor
public class RecipeCommentController {

    private final RecipeCommentService recipeCommentService;

    @Operation(summary = "Lấy danh sách recipe comments (search + pagination + sorting)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecipeCommentResponse>>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        PageResponse<RecipeCommentResponse> resp = recipeCommentService.getAll(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe comment theo id")
    @GetMapping("/{commentId}")
    public ResponseEntity<ApiResponse<RecipeCommentResponse>> getById(@PathVariable UUID commentId) {
        return ResponseEntity.ok(ApiResponse.success(recipeCommentService.getById(commentId)));
    }

    @Operation(summary = "Lấy recipe comments theo recipeId (search + pagination + sorting)")
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<PageResponse<RecipeCommentResponse>>> getByRecipeId(
            @PathVariable UUID recipeId,
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {


        PageResponse<RecipeCommentResponse> resp = recipeCommentService.getByRecipeId(recipeId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe comments theo userId (search + pagination + sorting)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<RecipeCommentResponse>>> getByUserId(
            @PathVariable UUID userId,
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        PageResponse<RecipeCommentResponse> resp = recipeCommentService.getByUserId(userId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe comments theo parentId (search + pagination + sorting)")
    @GetMapping("/reply/{parentId}")
    public ResponseEntity<ApiResponse<PageResponse<RecipeCommentResponse>>> getReplies(
            @PathVariable UUID parentId,
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {


        PageResponse<RecipeCommentResponse> resp = recipeCommentService.getReplies(parentId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Tạo recipe comment mới")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeCommentResponse>> create(@Valid @RequestBody RecipeCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeCommentService.create(request)));
    }

    @Operation(summary = "Cập nhật recipe comment")
    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<RecipeCommentResponse>> update(@PathVariable UUID commentId,
                                                                     @Valid @RequestBody RecipeCommentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeCommentService.update(commentId, request)));
    }

    @Operation(summary = "Xóa recipe comment")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID commentId) {
        recipeCommentService.delete(commentId);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}