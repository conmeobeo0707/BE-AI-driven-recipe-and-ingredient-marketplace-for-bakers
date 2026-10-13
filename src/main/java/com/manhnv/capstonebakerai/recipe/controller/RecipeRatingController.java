package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeRatingRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeRatingResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeRatingService;
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
@RequestMapping("/api/v1/recipe-ratings")
@RequiredArgsConstructor
public class RecipeRatingController {

    private final RecipeRatingService recipeRatingService;

    @Operation(summary = "Lấy danh sách ratings (search + pagination + sorting)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecipeRatingResponse>>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        PageResponse<RecipeRatingResponse> resp = recipeRatingService.getAll(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe rating theo id")
    @GetMapping("/{ratingId}")
    public ResponseEntity<ApiResponse<RecipeRatingResponse>> getById(@PathVariable UUID ratingId) {
        return ResponseEntity.ok(ApiResponse.success(recipeRatingService.getById(ratingId)));
    }

    @Operation(summary = "Lấy recipe ratings theo customerId (search + pagination + sorting)")
    @GetMapping("/user/{customerId}")
    public ResponseEntity<ApiResponse<PageResponse<RecipeRatingResponse>>> getByUserId(
            @PathVariable UUID customerId,
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        PageResponse<RecipeRatingResponse> resp = recipeRatingService.getByCustomerId(customerId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe ratings theo recipeId (search + pagination + sorting)")
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<PageResponse<RecipeRatingResponse>>> getByRecipeId(
            @PathVariable UUID recipeId,
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC)
            }) Pageable pageable) {

        PageResponse<RecipeRatingResponse> resp = recipeRatingService.getByRecipeId(recipeId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Tạo recipe rating mới")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeRatingResponse>> create(@Valid @RequestBody RecipeRatingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeRatingService.create(request)));
    }

    @Operation(summary = "Cập nhật recipe rating")
    @PutMapping("/{ratingId}")
    public ResponseEntity<ApiResponse<RecipeRatingResponse>> update(@PathVariable UUID ratingId,
                                                                    @Valid @RequestBody RecipeRatingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeRatingService.update(ratingId, request)));
    }

    @Operation(summary = "Xóa recipe rating")
    @DeleteMapping("/{ratingId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID ratingId) {
        recipeRatingService.delete(ratingId);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}