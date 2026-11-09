package com.manhnv.capstonebakerai.recommendation.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.recommendation.dto.RecipeRecommendationResponse;
import com.manhnv.capstonebakerai.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Validated
@Tag(name = "RecipeRecommendationController", description = "Gợi ý recipe cho CUSTOMER bằng hệ gợi ý tự xây dựng.")
public class RecipeRecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "Lấy recipe được gợi ý cho customer đang đăng nhập",
            description = """
                    Chỉ CUSTOMER. count mặc định 10, từ 1 đến 20.
                    Điểm (0-1) kết hợp 5 yếu tố: món giống món bạn hay tương tác (content), hợp trình độ (skill),
                    hợp sở thích (hobby), người có gu giống bạn cũng thích (collaborative), được đánh giá cao (quality).
                    Không gợi ý lại recipe đã bookmark hoặc đã rating; tối đa 3 recipe mỗi loại bánh.
                    Customer chưa có lịch sử vẫn có kết quả (dựa trên trình độ, sở thích, đánh giá).
                    Mỗi phần tử gồm recipe, score và 1-2 reasons. modelVersion = null nghĩa là đang dùng trọng số mặc định.
                    Kết quả giữ 5 phút, làm mới ngay khi customer xem, lưu, đánh giá hoặc sửa hồ sơ.
                    FE gửi recommId kèm khi customer xem/lưu/đánh giá recipe từ danh sách này.
                    """
    )
    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse<RecipeRecommendationResponse>> recommendRecipes(
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int count
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.recommend(SecurityUtils.getCurrentUserId(), count)));
    }
}
