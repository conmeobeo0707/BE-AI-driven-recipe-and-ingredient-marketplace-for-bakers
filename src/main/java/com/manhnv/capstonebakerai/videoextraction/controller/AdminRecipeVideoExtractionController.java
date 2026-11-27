package com.manhnv.capstonebakerai.videoextraction.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.videoextraction.dto.RecipeVideoExtractionRequest;
import com.manhnv.capstonebakerai.videoextraction.dto.RecipeVideoExtractionResponse;
import com.manhnv.capstonebakerai.videoextraction.service.RecipeVideoExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/recipes/video-extractions")
@RequiredArgsConstructor
@Tag(name = "AdminRecipeVideoExtractionController", description = "Admin dùng AI (Gemini) đọc video, chỉ trả kết quả.")
public class AdminRecipeVideoExtractionController {

    private final RecipeVideoExtractionService extractionService;

    @PostMapping("/preview")
    @Operation(
            summary = "Tóm tắt video thành recipe (không lưu DB)",
            description = """
                    Role ADMIN, MANAGER, STAFF hoặc BAKER_STAFF. mediaId là media VIDEO đã upload qua POST /api/v1/media/videos.
                    Không lưu gì vào DB.
                    Kết quả có thông tin khớp category/ingredient/tool theo cùng cách API customer so khớp khi lưu
                    (categoryId là category "Khác" nếu không khớp và category đó đã tồn tại).
                    isRecipe=false khi video không phải hướng dẫn làm món: các field khác null, list rỗng.
                    """
    )
    public ApiResponse<RecipeVideoExtractionResponse> preview(@Valid @RequestBody RecipeVideoExtractionRequest request) {
        return ApiResponse.success(extractionService.preview(request.mediaId()));
    }
}
