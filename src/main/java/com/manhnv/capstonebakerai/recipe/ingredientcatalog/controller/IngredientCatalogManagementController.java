package com.manhnv.capstonebakerai.recipe.ingredientcatalog.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.*;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientBackfillService;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/management/ingredient-catalog")
@RequiredArgsConstructor
@Tag(name = "IngredientCatalogManagementController", description = "ADMIN/BAKER_STAFF quản lý catalog, alias và duyệt backfill. Không xóa cứng catalog, không dùng AI sinh alias.")
public class IngredientCatalogManagementController {

    private final IngredientCatalogService catalogService;
    private final IngredientBackfillService backfillService;

    @GetMapping
    @Operation(summary = "Xem catalog", description = "active tùy chọn: true/false để lọc; bỏ qua lấy cả hai. Danh sách theo tên chuẩn.")
    public ApiResponse<List<IngredientCatalogResponse>> getAll(@RequestParam(required = false) Boolean active) {
        return ApiResponse.success(catalogService.getAll(active));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem catalog theo ID", description = "ID là UUID; trả cả catalog inactive. ID không tồn tại trả 404.")
    public ApiResponse<IngredientCatalogResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(catalogService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo nguyên liệu chuẩn", description = "canonicalName bắt buộc, tối đa 150 ký tự và phải còn chữ/số sau chuẩn hóa. Mặc định active=true. Tạo alias tự thân cùng transaction. Tên chuẩn hóa thuộc catalog khác, kể cả inactive, trả 409.")
    public ApiResponse<IngredientCatalogResponse> create(@Valid @RequestBody SaveIngredientCatalogRequest request) {
        return ApiResponse.success(catalogService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa tên chuẩn", description = "canonicalName bắt buộc, tối đa 150 ký tự. Giữ tên cũ thành alias; ID và mapping recipe không thay đổi. Không tự dịch hoặc gộp nguyên liệu.")
    public ApiResponse<IngredientCatalogResponse> update(@PathVariable UUID id, @Valid @RequestBody SaveIngredientCatalogRequest request) {
        return ApiResponse.success(catalogService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Bật/tắt catalog", description = "active bắt buộc. Tắt catalog đang được recipe PUBLISHED sử dụng trả 409. Catalog inactive và alias của nó không được resolve hoặc chọn cho suggestion.")
    public ApiResponse<IngredientCatalogResponse> setActive(@PathVariable UUID id, @Valid @RequestBody UpdateIngredientCatalogActiveRequest request) {
        return ApiResponse.success(catalogService.setActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ngừng sử dụng catalog", description = "Đặt active=false, không xóa cứng. Quy tắc giống PATCH active=false; recipe PUBLISHED đang sử dụng sẽ chặn thao tác.")
    public ApiResponse<IngredientCatalogResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.success(catalogService.setActive(id, false));
    }

    @GetMapping("/{id}/aliases")
    @Operation(summary = "Xem alias", description = "Danh sách alias của catalog, gồm alias tự thân. Cho phép xem catalog inactive.")
    public ApiResponse<List<IngredientAliasResponse>> aliases(@PathVariable UUID id) {
        return ApiResponse.success(catalogService.getAliases(id));
    }

    @PostMapping("/{id}/aliases")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm alias", description = "alias bắt buộc, tối đa 150 ký tự. languageCode tùy chọn/null, tối đa 10 ký tự, ví dụ vi/en/en-US. Alias trùng sau chuẩn hóa trả 409. Thêm alias không tự sửa các nguyên liệu recipe cũ.")
    public ApiResponse<IngredientAliasResponse> addAlias(@PathVariable UUID id, @Valid @RequestBody SaveIngredientAliasRequest request) {
        return ApiResponse.success(catalogService.addAlias(id, request));
    }

    @PutMapping("/{id}/aliases/{aliasId}")
    @Operation(summary = "Sửa alias", description = "alias bắt buộc, tối đa 150 ký tự; languageCode có thể null. Không đổi alias tự thân sang tên chuẩn hóa khác. Tên mới không được thuộc catalog/alias khác. Tên alias cũ sẽ ngừng resolve; mapping recipe theo ID không đổi.")
    public ApiResponse<IngredientAliasResponse> updateAlias(@PathVariable UUID id, @PathVariable UUID aliasId,
                                                          @Valid @RequestBody SaveIngredientAliasRequest request) {
        return ApiResponse.success(catalogService.updateAlias(id, aliasId, request));
    }

    @DeleteMapping("/{id}/aliases/{aliasId}")
    @Operation(summary = "Xóa alias", description = "Không được xóa alias ứng với tên chuẩn hiện tại (409). Xóa alias không xóa catalog hoặc mapping recipe đã lưu; chỉ ngừng nhận diện cách gọi đó.")
    public ApiResponse<Void> deleteAlias(@PathVariable UUID id, @PathVariable UUID aliasId) {
        catalogService.deleteAlias(id, aliasId);
        return ApiResponse.success(null);
    }

    @GetMapping("/backfill/preview")
    @Operation(summary = "Xem trước nguyên liệu chưa mapping", description = "limit mặc định 100, từ 1 đến 200. proposedCatalogIngredientId chỉ được điền khi exact resolve thành công. candidates là gợi ý cần duyệt, không tự apply. totalUnmapped là tổng còn thiếu; items là lô hiện tại.")
    public ApiResponse<IngredientBackfillPreviewResponse> preview(@RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(backfillService.preview(limit));
    }

    @PostMapping("/backfill")
    @Operation(summary = "Áp dụng mapping đã duyệt", description = "mappings gồm 1 đến 200 phần tử: ingredientId, catalogIngredientId và expectedName lấy từ ingredientName của preview. Chỉ mapping các dòng còn null và giữ nguyên tên; dòng đã mapping/đổi tên/xóa trả 409. Catalog phải active. Toàn bộ lô cùng transaction; không thay đổi tên, lượng hoặc đơn vị.")
    public ApiResponse<Map<String, Integer>> apply(@Valid @RequestBody IngredientBackfillRequest request) {
        return ApiResponse.success(Map.of("updatedCount", backfillService.apply(request)));
    }
}
