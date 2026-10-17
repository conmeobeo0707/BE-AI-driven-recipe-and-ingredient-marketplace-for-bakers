package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.ToolRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.ToolResponse;
import com.manhnv.capstonebakerai.recipe.service.ToolService;
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
@RequestMapping("/api/v1/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolService toolService;

    @Operation(summary = "Lấy danh sách tools (search + pagination + sorting)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ToolResponse>>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "name", direction = Sort.Direction.ASC)
            }) Pageable pageable) {

        PageResponse<ToolResponse> resp = toolService.getAll(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Lấy tool theo id")
    @GetMapping("/{toolId}")
    public ResponseEntity<ApiResponse<ToolResponse>> getById(@PathVariable UUID toolId) {
        return ResponseEntity.ok(ApiResponse.success(toolService.getById(toolId)));
    }

    @Operation(summary = "Lấy tools theo recipe id (search + pagination + sorting)")
    @GetMapping("/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<PageResponse<ToolResponse>>> getByRecipeId(
            @PathVariable UUID recipeId,
            @RequestParam(value = "search", required = false) String search,
            @ParameterObject
            @PageableDefault(page = 0, size = 10)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "name", direction = Sort.Direction.ASC)
            }) Pageable pageable) {

        PageResponse<ToolResponse> resp = toolService.getByRecipeId(recipeId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @Operation(summary = "Tạo tool mới", description = "Request body: recipeId và name bắt buộc, không được null. recipeId phải trỏ tới recipe hiện có.")
    @PostMapping
    public ResponseEntity<ApiResponse<ToolResponse>> create(@Valid @RequestBody ToolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(toolService.create(request)));
    }

    @Operation(summary = "Cập nhật tool", description = "Request body: recipeId và name bắt buộc, không được null. Đây là cập nhật các giá trị, không phải partial update.")
    @PutMapping("/{toolId}")
    public ResponseEntity<ApiResponse<ToolResponse>> update(@PathVariable UUID toolId,
                                                            @Valid @RequestBody ToolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(toolService.update(toolId, request)));
    }

    @Operation(summary = "Xóa tool")
    @DeleteMapping("/{toolId}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable UUID toolId) {
        toolService.delete(toolId);
        return ResponseEntity.ok(ApiResponse.success("Deleted"));
    }
}
