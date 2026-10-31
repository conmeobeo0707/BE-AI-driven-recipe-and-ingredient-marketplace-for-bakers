package com.manhnv.capstonebakerai.recipe.suggestion.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientImageRecognitionResponse;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientSuggestionRequest;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientSuggestionResponse;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.ConfirmIngredientSuggestionRequest;
import com.manhnv.capstonebakerai.recipe.suggestion.service.IngredientImageService;
import com.manhnv.capstonebakerai.recipe.suggestion.service.RecipeSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/recipe-suggestions")
@RequiredArgsConstructor
@Tag(
        name = "RecipeSuggestionController",
        description = "Tìm recipe phù hợp với nguyên liệu Customer nhập."
)
public class RecipeSuggestionController {

    private final RecipeSuggestionService recipeSuggestionService;
    private final IngredientImageService ingredientImageService;

    @PostMapping("/ingredients")
    @Operation(
            summary = "Tìm recipe từ tên nguyên liệu",
            description = """
                    Chỉ CUSTOMER được gọi.
                    ingredients gồm 1 đến 20 tên nguyên liệu.
                    limit có thể bỏ qua, mặc định 3, tối đa 5.
                    Tên được resolve bằng catalog và alias.
                    Nguyên liệu chưa nhận ra được trả trong unmatchedIngredients.
                    Mỗi nguyên liệu chưa khớp có candidates để chọn lại; fuzzy không tự gán.
                    allowPartial mặc định false: còn unmatched sẽ trả requiresResolution=true
                    và recipes rỗng. Chỉ đặt true khi người dùng chấp nhận bỏ qua phần chưa khớp.
                    Coverage tính theo loại nguyên liệu, không tính số lượng.
                    """
    )
    public ApiResponse<IngredientSuggestionResponse> suggestFromIngredients(
            @Valid @RequestBody IngredientSuggestionRequest request
    ) {
        return ApiResponse.success(
                recipeSuggestionService.suggest(
                        request.ingredients(),
                        request.limit(),
                        "TEXT",
                        Boolean.TRUE.equals(request.allowPartial())
                )
        );
    }

    @PostMapping(
            value = "/images/recognize",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Nhận diện nguyên liệu từ ảnh",
            description = """
                Chỉ CUSTOMER được gọi.
                Gửi multipart/form-data với field file.
                Hỗ trợ JPEG/PNG, tối đa 5 MB và 16 triệu pixel.
                Ảnh không được lưu lên R2.
                Kết quả có thể chưa chính xác; Customer cần kiểm tra,
                sửa và xác nhận trước khi tìm recipe.
                matched chỉ thể hiện tên resolve được trong catalog.
                Item chưa matched có candidates để người dùng chọn catalog ID.
                Danh sách rỗng nghĩa là chưa nhận diện được nguyên liệu.
                """
    )
    public ApiResponse<IngredientImageRecognitionResponse> recognizeImage(
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.success(
                ingredientImageService.recognize(file)
        );
    }

    @PostMapping("/images/confirm")
    @Operation(
            summary = "Tìm recipe từ nguyên liệu đã xác nhận sau nhận diện ảnh",
            description = """
                Customer kiểm tra ảnh và gửi catalogIngredientIds gồm 1 đến 20 UUID
                đã chọn, không gửi tên để resolve lại. Có thể lấy ID từ kết quả ảnh
                hoặc GET /api/v1/ingredient-catalog. ID thiếu/inactive trả 400.
                unmatchedIngredients tùy chọn là các tên còn chưa chọn được, mỗi tên
                tối đa 120 ký tự. Tổng số ID và tên chưa chọn không quá 20.
                allowPartial mặc định false. Nếu còn unmatched thì trả
                requiresResolution=true và recipes rỗng. Đặt true khi người dùng
                chấp nhận tìm chỉ bằng các ID đã chọn. Tên unmatched không tham gia matching.
                limit mặc định 3, tối đa 5.
                API này dùng lại logic tìm recipe hiện có và không gọi AI.
                """
    )
    public ApiResponse<IngredientSuggestionResponse> confirmImageIngredients(
            @Valid @RequestBody ConfirmIngredientSuggestionRequest request
    ) {
        return ApiResponse.success(
                recipeSuggestionService.suggestByIds(
                        request.catalogIngredientIds(),
                        request.unmatchedIngredients(),
                        request.limit(),
                        "IMAGE",
                        Boolean.TRUE.equals(request.allowPartial())
                )
        );
    }

    @PostMapping("/ingredients/confirm")
    @Operation(summary = "Tìm recipe bằng catalog ID sau khi sửa input text", description = "Body và quy tắc giống /images/confirm; inputMode trả về là TEXT. Không gọi AI. allowPartial mặc định false.")
    public ApiResponse<IngredientSuggestionResponse> confirmTextIngredients(
            @Valid @RequestBody ConfirmIngredientSuggestionRequest request
    ) {
        return ApiResponse.success(recipeSuggestionService.suggestByIds(
                request.catalogIngredientIds(), request.unmatchedIngredients(), request.limit(),
                "TEXT", Boolean.TRUE.equals(request.allowPartial())
        ));
    }
}
