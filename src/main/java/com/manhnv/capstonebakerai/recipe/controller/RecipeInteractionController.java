package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeInteractionResponse;
import com.manhnv.capstonebakerai.recipe.enums.InteractionSource;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.recipe.service.RecipeInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers/me/interaction-history")
@RequiredArgsConstructor
@Validated
public class RecipeInteractionController {

    private final RecipeInteractionService interactionService;

    @Operation(summary = "Lấy lịch sử tương tác của customer đang đăng nhập")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecipeInteractionResponse>>> getHistory(
            @RequestParam(required = false) RecipeInteractionType type,
            @ParameterObject
            @PageableDefault(page = 0, size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                interactionService.getCurrentCustomerHistory(type, pageable)));
    }

    @Operation(summary = "Ghi nhận customer đã xem recipe")
    @PostMapping("/views/{recipeId}")
    public ResponseEntity<ApiResponse<String>> recordView(
            @PathVariable UUID recipeId,
            @RequestParam(defaultValue = "DIRECT") InteractionSource source,
            @RequestParam(required = false) @Size(max = 100) String recommId
    ) {
        interactionService.recordViewForCurrentCustomer(recipeId, source, recommId);
        return ResponseEntity.ok(ApiResponse.success("View recorded"));
    }
}
