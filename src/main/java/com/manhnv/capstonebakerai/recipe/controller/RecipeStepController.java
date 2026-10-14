package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeStepResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeStepService;
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
@RequestMapping("/api/v1/recipe-steps")
@RequiredArgsConstructor
public class RecipeStepController {

    private final RecipeStepService recipeStepService;

    @Operation(summary = "Lấy danh sách recipe steps (search + pagination + sorting)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecipeStepResponse>>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "stepNumber", direction = Sort.Direction.ASC)
            }) Pageable pageable) {

        PageResponse<RecipeStepResponse> resp = recipeStepService.getAllRecipeSteps(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy recipe step theo id")
    @GetMapping("/{recipeStepId}")
    public ResponseEntity<ApiResponse<RecipeStepResponse>> getById(@PathVariable UUID recipeStepId) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepService.getRecipeStepById(recipeStepId)));
    }

    @Operation(summary = "Lấy recipe steps theo recipeId (search + pagination + sorting)")
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<PageResponse<RecipeStepResponse>>> getByRecipeId(
            @PathVariable UUID recipeId,
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "stepNumber", direction = Sort.Direction.ASC)
            }) Pageable pageable) {

        PageResponse<RecipeStepResponse> resp = recipeStepService.getByRecipeId(recipeId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Tạo recipe step mới")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeStepResponse>> create(@Valid @RequestBody RecipeStepRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepService.createRecipeStep(request)));
    }

    @Operation(summary = "Cập nhật recipe step")
    @PutMapping("/{recipeStepId}")
    public ResponseEntity<ApiResponse<RecipeStepResponse>> update(@PathVariable UUID recipeStepId,
                                                                  @Valid @RequestBody RecipeStepRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeStepService.updateRecipeStep(recipeStepId, request)));
    }

    @Operation(summary = "Xóa recipe step")
    @DeleteMapping("/{recipeStepId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID recipeStepId) {
        recipeStepService.deleteRecipeStep(recipeStepId);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}
