package com.manhnv.capstonebakerai.recipe.ingredientcatalog.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.*;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientCatalogService;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/management/ingredient-catalog")
@RequiredArgsConstructor
@Tag(name = "IngredientCatalogManagementController", description = "Chỉ BAKER_STAFF quản lý catalog, alias và nối nguyên liệu recipe với catalog. Không xóa cứng catalog, không dùng AI sinh alias.")
public class IngredientCatalogManagementController {

    private final IngredientCatalogService catalogService;
    private final IngredientLinkService linkService;

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
    @Operation(summary = "Tạo nguyên liệu chuẩn", description = "canonicalName bắt buộc, tối đa 150 ký tự và phải còn chữ/số sau chuẩn hóa. Mặc định active=true. Tạo alias tự thân cùng transaction. Tên chuẩn hóa thuộc catalog khác, kể cả inactive, trả 409. Nguyên liệu recipe chưa nối có tên trùng tên chuẩn được nối luôn. Muốn nối thêm cách gọi khác, dùng POST /ingredient-links/by-name hoặc thêm alias.")
    public ApiResponse<IngredientCatalogResponse> create(@Valid @RequestBody SaveIngredientCatalogRequest request) {
        return ApiResponse.success(catalogService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa tên chuẩn", description = "canonicalName bắt buộc, tối đa 150 ký tự. Giữ tên cũ thành alias; ID và mapping recipe không thay đổi. Nguyên liệu recipe chưa nối có tên trùng tên mới được nối luôn. Không tự dịch hoặc gộp nguyên liệu.")
    public ApiResponse<IngredientCatalogResponse> update(@PathVariable UUID id, @Valid @RequestBody SaveIngredientCatalogRequest request) {
        return ApiResponse.success(catalogService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Bật/tắt catalog", description = "active bắt buộc. Không có xóa cứng: ngừng sử dụng catalog bằng active=false. Tắt catalog đang được recipe PUBLISHED sử dụng trả 409. Catalog inactive và alias của nó không được resolve hoặc chọn cho suggestion. Bật lại (active=true) thì nối luôn nguyên liệu recipe chưa nối có tên trùng tên chuẩn hoặc alias.")
    public ApiResponse<IngredientCatalogResponse> setActive(@PathVariable UUID id, @Valid @RequestBody UpdateIngredientCatalogActiveRequest request) {
        return ApiResponse.success(catalogService.setActive(id, request.active()));
    }

    @GetMapping("/{id}/aliases")
    @Operation(summary = "Xem alias", description = "Danh sách alias của catalog, gồm alias tự thân. Cho phép xem catalog inactive.")
    public ApiResponse<List<IngredientAliasResponse>> aliases(@PathVariable UUID id) {
        return ApiResponse.success(catalogService.getAliases(id));
    }

    @PostMapping("/{id}/aliases")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Thêm alias", description = "alias bắt buộc, tối đa 150 ký tự. languageCode tùy chọn/null, tối đa 10 ký tự, ví dụ vi/en/en-US. Alias trùng sau chuẩn hóa trả 409. Nếu catalog active, mọi nguyên liệu recipe chưa nối có tên trùng alias (sau chuẩn hóa, khớp chính xác) được nối luôn; dòng đã nối không bị đổi.")
    public ApiResponse<IngredientAliasResponse> addAlias(@PathVariable UUID id, @Valid @RequestBody SaveIngredientAliasRequest request) {
        return ApiResponse.success(catalogService.addAlias(id, request));
    }

    @PutMapping("/{id}/aliases/{aliasId}")
    @Operation(summary = "Sửa alias", description = "alias bắt buộc, tối đa 150 ký tự; languageCode có thể null. Không đổi alias tự thân sang tên chuẩn hóa khác. Tên mới không được thuộc catalog/alias khác. Tên alias cũ sẽ ngừng resolve; mapping recipe theo ID không đổi. Nguyên liệu recipe chưa nối có tên trùng tên alias mới được nối luôn.")
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

    @GetMapping("/ingredient-links")
    @Operation(summary = "Xem tên nguyên liệu chưa nối, gom theo tên", description = "Mỗi phần tử là một tên (sau chuẩn hóa) còn dòng nguyên liệu recipe chưa nối: name là cách viết phổ biến nhất, spellings là mọi cách viết, rowCount là số dòng, candidates là tối đa 3 catalog gợi ý (chỉ để staff chọn, không tự nối). Sắp xếp theo rowCount giảm dần. keyword tùy chọn (so sau chuẩn hóa), tối đa 150 ký tự; page từ 0, size 1 đến 100 (mặc định 20). Xử lý bằng POST /ingredient-links/by-name (tạo catalog trước nếu chưa có).")
    public ApiResponse<PageResponse<UnlinkedIngredientGroup>> unlinkedGroups(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(linkService.unlinkedGroups(keyword, page, size));
    }

    @PostMapping("/ingredient-links/by-name")
    @Operation(summary = "Nối mọi nguyên liệu cùng tên với catalog", description = "name bắt buộc, tối đa 150 ký tự (thường lấy từ name của GET /ingredient-links); catalogIngredientId là catalog active. Nếu name chưa là tên chuẩn/alias của catalog đó thì thêm thành alias, để recipe sau này tự khớp. Sau đó nối mọi dòng chưa nối có tên trùng sau chuẩn hóa. name đã thuộc catalog khác trả 409; catalog không tồn tại hoặc inactive trả 400. Response có aliasCreated và linkedIngredientCount.")
    public ApiResponse<LinkByNameResponse> linkByName(@Valid @RequestBody LinkByNameRequest request) {
        return ApiResponse.success(catalogService.linkByName(request));
    }
}
