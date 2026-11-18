package com.manhnv.capstonebakerai.recommendation.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.recommendation.dto.KnowledgeResponse;
import com.manhnv.capstonebakerai.recommendation.dto.TrainingResponse;
import com.manhnv.capstonebakerai.recommendation.service.RecommendationAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/recommendations")
@RequiredArgsConstructor
@Tag(name = "AdminRecommendationController", description = "ADMIN huấn luyện model gợi ý và xem kho tri thức.")
public class AdminRecommendationController {

    private final RecommendationAdminService adminService;

    @PostMapping("/models/train")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Huấn luyện model gợi ý", description = """
            Học trọng số của 5 đặc trưng (content, skill, hobby, collaborative, quality) bằng logistic regression
            từ lượt thích thật (bookmark hoặc rating 4-5 sao).
            Đánh giá: với customer có từ 3 lượt thích, giấu lượt cuối và kiểm tra recipe đó có nằm trong top k không.
            So sánh baseline (chỉ đánh giá + độ phổ biến), defaultWeights (cấu hình) và trained (vừa học).
            Nếu trained tốt hơn defaultWeights (Hit@k, rồi MRR) thì model được kích hoạt ngay (active = true).
            k từ 1 đến 20, mặc định 10. Thiếu dữ liệu (mặc định cần 20 customer có từ 3 lượt thích
            và 100 lượt thích để học) trả 400 kèm số liệu hiện có.
            """)
    public ApiResponse<TrainingResponse> train(@RequestParam(defaultValue = "10") int k) {
        return ApiResponse.success(adminService.train(k, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/knowledge")
    @Operation(summary = "Xem kho tri thức", description = """
            Tri thức lưu sẵn: số nguyên liệu catalog, alias, tỉ lệ nguyên liệu recipe đã nối catalog,
            bảng trình độ - độ khó (skillFit).
            Tri thức tự học: learnedHobbies - loại bánh mà người có cùng hobby thích nhiều hơn mức trung bình
            (lift = gấp bao nhiêu lần trung bình, score 0-1 là độ mạnh khi gợi ý; hobby dưới 5 lượt thích chưa học).
            Kèm model đang dùng (activeModelVersion, null = trọng số mặc định) và trọng số đang dùng.
            Dữ liệu cập nhật tối đa sau 5 phút.
            """)
    public ApiResponse<KnowledgeResponse> knowledge() {
        return ApiResponse.success(adminService.knowledge());
    }
}
