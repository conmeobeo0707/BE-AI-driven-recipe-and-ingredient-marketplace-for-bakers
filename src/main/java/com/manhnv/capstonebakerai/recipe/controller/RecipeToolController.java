package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeToolResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeToolService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipe-tools")
@RequiredArgsConstructor
public class RecipeToolController {

    private final RecipeToolService recipeToolService;

    @Operation(summary = "Lấy danh sách recipe tools")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipeToolResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(recipeToolService.getAll()));
    }

    @Operation(summary = "Lấy recipe tool theo id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeToolResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recipeToolService.getById(id)));
    }

    @Operation(summary = "Lấy recipe tools theo recipeId")
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<List<RecipeToolResponse>>> getByRecipe(@PathVariable UUID recipeId) {
        return ResponseEntity.ok(ApiResponse.success(recipeToolService.getByRecipe(recipeId)));
    }

    @Operation(summary = "Tạo recipe tool")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeToolResponse>> create(@Valid @RequestBody RecipeToolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeToolService.create(request)));
    }

    @Operation(summary = "Cập nhật recipe tool")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeToolResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody RecipeToolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeToolService.update(id, request)));
    }

    @Operation(summary = "Xóa recipe tool")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID id) {
        recipeToolService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}
