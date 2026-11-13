package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepToolResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepToolService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipe-step-tools")
@RequiredArgsConstructor
public class RecipeStepToolController {

    private final RecipeStepToolService recipeStepToolService;

    @Operation(summary = "Lấy danh sách recipe step tools")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipeStepToolResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(recipeStepToolService.getAll()));
    }

    @Operation(summary = "Lấy recipe step tool theo id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeStepToolResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepToolService.getById(id)));
    }

    @Operation(summary = "Lấy recipe step tools theo recipeStepId")
    @GetMapping("/recipe-step/{recipeStepId}")
    public ResponseEntity<ApiResponse<List<RecipeStepToolResponse>>> getByRecipeStep(@PathVariable UUID recipeStepId) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepToolService.getByRecipeStep(recipeStepId)));
    }

    @Operation(summary = "Tạo recipe step tool")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeStepToolResponse>> create(@Valid @RequestBody RecipeStepToolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepToolService.create(request)));
    }

    @Operation(summary = "Cập nhật recipe step tool")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeStepToolResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody RecipeStepToolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepToolService.update(id, request)));
    }

    @Operation(summary = "Xóa recipe step tool")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID id) {
        recipeStepToolService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}
