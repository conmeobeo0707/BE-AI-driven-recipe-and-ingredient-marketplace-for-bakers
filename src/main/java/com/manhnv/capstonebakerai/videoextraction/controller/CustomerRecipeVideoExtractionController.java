package com.manhnv.capstonebakerai.videoextraction.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.videoextraction.dto.RecipeVideoExtractionRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
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
@RequestMapping("/api/v1/recipes/video-extractions")
@RequiredArgsConstructor
@Tag(name = "CustomerRecipeVideoExtractionController", description = "Customer dùng AI (Gemini) đọc video và lưu thành recipe.")
public class CustomerRecipeVideoExtractionController {

    private final RecipeVideoExtractionService extractionService;

    @PostMapping
    @Operation(
            summary = "Tóm tắt video thành recipe và lưu vào DB",
            description = """
                    Role CUSTOMER. mediaId là media VIDEO đã upload qua POST /api/v1/media/videos (mp4, mov, webm).
                    Lưu recipe (status PERSONAL), recipe_steps, recipe_ingredients, recipe_tools. Response giống POST /api/v1/recipes (RecipeResponse của recipe vừa tạo).
                    Video không phải hướng dẫn làm món trả lỗi 422 và không lưu gì.
                    Category: tên trùng (không phân biệt hoa thường, dấu) với category có sẵn thì gắn category đó,
                    không thì gắn category "Khác".
                    Ingredient: so tên với ingredients và ingredient_aliases; Tool: so tên với tools.
                    Khớp thì lưu ingredientId/toolId và raw_name là tên trong DB; không khớp thì ingredientId/toolId null
                    và raw_name là tên AI trả về.
                    Liên kết nguyên liệu/dụng cụ của từng bước trỏ tới dòng recipe_ingredients/recipe_tools nên lưu được cả item không khớp.
                    Trừ điểm loyalty theo app.loyalty.recipe-video-price (mặc định 5) trước khi gọi AI; không đủ điểm trả 402.
                    AI lỗi (quá tải, timeout, kết quả không đọc được) hoặc lưu thất bại thì hoàn điểm.
                    AI từ chối video hoặc video không phải công thức (422) thì không hoàn điểm.
                    Video không hợp lệ (không tìm thấy, sai định dạng, quá lớn) bị từ chối trước khi trừ điểm.
                    Gọi AI có thể mất 30 giây đến vài phút tuỳ độ dài video.
                    """
    )
    public ApiResponse<RecipeResponse> extractAndSave(@Valid @RequestBody RecipeVideoExtractionRequest request) {
        return ApiResponse.success(extractionService.extractAndSave(request.mediaId()));
    }
}
