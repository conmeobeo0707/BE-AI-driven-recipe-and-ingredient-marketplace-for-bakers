package com.manhnv.capstonebakerai.recipe.ingredientalias.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.IngredientAliasResponse;
import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.SaveIngredientAliasRequest;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientAliasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingredients/{ingredientId}/aliases")
@RequiredArgsConstructor
@Tag(name = "IngredientAliasController", description = "STAFF quản lý tên gọi khác (alias) của nguyên liệu. "
        + "Alias giúp nhận diện ảnh và ô tìm nguyên liệu khớp được tên tiếng Anh hoặc cách viết khác.")
public class IngredientAliasController {

    private final IngredientAliasService aliasService;

    @GetMapping
    @Operation(summary = "Xem alias của nguyên liệu", description = "Nguyên liệu không tồn tại trả 404.")
    public ApiResponse<List<IngredientAliasResponse>> getAll(@PathVariable UUID ingredientId) {
        return ApiResponse.success(aliasService.getAliases(ingredientId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm alias", description = """
            alias bắt buộc, tối đa 150 ký tự, phải còn chữ hoặc số sau chuẩn hoá.
            languageCode tuỳ chọn, tối đa 10 ký tự, ví dụ vi, en, en-US.
            Tên (sau chuẩn hoá: bỏ dấu, chữ thường) đã là alias của nguyên liệu này trả 409 ALIAS_ALREADY_EXISTS;
            đã là tên hoặc alias của nguyên liệu khác trả 409 NAME_ALREADY_IN_USE.
            """)
    public ApiResponse<IngredientAliasResponse> add(@PathVariable UUID ingredientId,
                                                    @Valid @RequestBody SaveIngredientAliasRequest request) {
        return ApiResponse.success(aliasService.add(ingredientId, request));
    }

    @PutMapping("/{aliasId}")
    @Operation(summary = "Sửa alias", description = "Cùng quy tắc với thêm alias. Alias không thuộc nguyên liệu này trả 404.")
    public ApiResponse<IngredientAliasResponse> update(@PathVariable UUID ingredientId, @PathVariable UUID aliasId,
                                                       @Valid @RequestBody SaveIngredientAliasRequest request) {
        return ApiResponse.success(aliasService.update(ingredientId, aliasId, request));
    }

    @DeleteMapping("/{aliasId}")
    @Operation(summary = "Xoá alias", description = "Chỉ ngừng nhận diện cách gọi đó; công thức không bị ảnh hưởng. "
            + "Alias không thuộc nguyên liệu này trả 404.")
    public ApiResponse<Void> delete(@PathVariable UUID ingredientId, @PathVariable UUID aliasId) {
        aliasService.delete(ingredientId, aliasId);
        return ApiResponse.success(null);
    }
}
