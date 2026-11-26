package com.manhnv.capstonebakerai.recipe.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.excel.RecipeExcelImportService;
import com.manhnv.capstonebakerai.recipe.excel.RecipeExcelTemplateService;
import com.manhnv.capstonebakerai.recipe.excel.dto.ImportResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF', 'BAKER_STAFF')")
public class RecipeExcelController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final RecipeExcelTemplateService templateService;
    private final RecipeExcelImportService importService;

    @Operation(summary = "Tải file Excel mẫu để import recipe",
            description = "File gồm sheet HuongDan, sheet mẫu MAU (có dropdown) và sheet ẩn MasterData lấy từ dữ liệu hiện tại.")
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> downloadTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("recipe_import_template.xlsx").build().toString())
                .contentType(XLSX)
                .body(templateService.buildTemplate());
    }

    @Operation(summary = "Import nhiều recipe từ file Excel",
            description = "Mỗi sheet (trừ HuongDan, MAU, MasterData) là một recipe. dryRun=true chỉ kiểm tra và trả bản xem trước, "
                    + "dryRun=false lưu tất cả ở trạng thái DRAFT. Có lỗi thì trả 400 kèm danh sách lỗi và không lưu gì.")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResultResponse>> importRecipes(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(ApiResponse.success(importService.importRecipes(file, dryRun)));
    }
}
