package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeIngredientResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeIngredientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipe-ingredients")
@RequiredArgsConstructor
public class RecipeIngredientController {

    private final RecipeIngredientService recipeIngredientService;

    @Operation(summary = "Lấy danh sách recipe ingredients")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipeIngredientResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(recipeIngredientService.getAll()));
    }

    @Operation(summary = "Lấy recipe ingredient theo id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeIngredientResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recipeIngredientService.getById(id)));
    }

    @Operation(summary = "Lấy recipe ingredients theo recipeId")
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<List<RecipeIngredientResponse>>> getByRecipe(@PathVariable UUID recipeId) {
        return ResponseEntity.ok(ApiResponse.success(recipeIngredientService.getByRecipe(recipeId)));
    }

    @Operation(summary = "Tạo recipe ingredient")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeIngredientResponse>> create(@Valid @RequestBody RecipeIngredientRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeIngredientService.create(request)));
    }

    @Operation(summary = "Cập nhật recipe ingredient")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeIngredientResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody RecipeIngredientRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeIngredientService.update(id, request)));
    }

    @Operation(summary = "Xóa recipe ingredient")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID id) {
        recipeIngredientService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}
