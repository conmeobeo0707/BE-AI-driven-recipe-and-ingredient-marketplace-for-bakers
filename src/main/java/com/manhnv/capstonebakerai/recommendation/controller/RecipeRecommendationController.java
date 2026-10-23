package com.manhnv.capstonebakerai.recommendation.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.recommendation.dto.RecipeRecommendationResponse;
import com.manhnv.capstonebakerai.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
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
public class RecipeRecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "Lấy danh sách recipe được đề xuất cho customer đang đăng nhập, mặc định 10 recipe, tối đa 20 recipe")
    @GetMapping("/recipes")
    public ResponseEntity<ApiResponse<RecipeRecommendationResponse>> recommendRecipes(
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int count
    ) {
        RecipeRecommendationResponse response = recommendationService.recommend(
                SecurityUtils.getCurrentUserId(), count);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
