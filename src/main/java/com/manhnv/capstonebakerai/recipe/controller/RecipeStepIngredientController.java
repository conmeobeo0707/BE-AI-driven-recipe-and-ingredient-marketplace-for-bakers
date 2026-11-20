package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepIngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepIngredientResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepIngredientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipe-step-ingredients")
@RequiredArgsConstructor
public class RecipeStepIngredientController {

    private final RecipeStepIngredientService recipeStepIngredientService;

    @Operation(summary = "Lấy danh sách recipe step ingredients")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipeStepIngredientResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(recipeStepIngredientService.getAll()));
    }

    @Operation(summary = "Lấy recipe step ingredient theo id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeStepIngredientResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepIngredientService.getById(id)));
    }

    @Operation(summary = "Lấy recipe step ingredients theo recipeStepId")
    @GetMapping("/recipe-step/{recipeStepId}")
    public ResponseEntity<ApiResponse<List<RecipeStepIngredientResponse>>> getByRecipeStep(@PathVariable UUID recipeStepId) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepIngredientService.getByRecipeStep(recipeStepId)));
    }

    @Operation(summary = "Tạo recipe step ingredient")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeStepIngredientResponse>> create(@Valid @RequestBody RecipeStepIngredientRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepIngredientService.create(request)));
    }

    @Operation(summary = "Cập nhật recipe step ingredient")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeStepIngredientResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody RecipeStepIngredientRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepIngredientService.update(id, request)));
    }

    @Operation(summary = "Xóa recipe step ingredient")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID id) {
        recipeStepIngredientService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}
