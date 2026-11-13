package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.request.CreateRecipeVideoRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeVideoRenderResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeVideoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeVideoController {

    private final RecipeVideoService recipeVideoService;

    @Operation(summary = "Tạo video cho recipe bằng Shotstack", description = "Nhận recipeId, lấy thông tin recipe, category, ảnh, tools, ingredients và các bước từ DB để Shotstack dựng video có giọng đọc tiếng Việt. Trả về renderId cùng trạng thái PROCESSING.")
    @PostMapping(value = "/videos/render", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<RecipeVideoRenderResponse>> createRecipeVideo(
            @Valid @RequestBody CreateRecipeVideoRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.success(recipeVideoService.createVideo(request)));
    }
}
