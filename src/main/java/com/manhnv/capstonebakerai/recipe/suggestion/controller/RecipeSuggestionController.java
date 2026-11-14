package com.manhnv.capstonebakerai.recipe.suggestion.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientCatalogCandidate;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientCatalogLookupService;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientImageRecognitionResponse;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.RecipeSuggestionRequest;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.RecipeSuggestionResponse;
import com.manhnv.capstonebakerai.recipe.suggestion.service.IngredientImageService;
import com.manhnv.capstonebakerai.recipe.suggestion.service.RecipeSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    private final IngredientCatalogLookupService lookupService;

    @GetMapping("/catalog-ingredients")
    @Operation(
            summary = "Tìm nguyên liệu chuẩn",
            description = """
                    Chỉ CUSTOMER được gọi. Ô gợi ý khi customer gõ nguyên liệu, hoặc khi chọn lại món ảnh chưa matched;
                    người dùng tìm và chọn catalogIngredientId rồi gửi sang POST /api/v1/recipe-suggestions.
                    keyword tùy chọn, tối đa 150 ký tự; bỏ trống để xem danh sách active theo tên,
                    khi đó matchType=null và similarity=0 vì không có so khớp.
                    limit mặc định 10, từ 1 đến 20. Tìm tên chuẩn và alias theo EXACT, PARTIAL, FUZZY;
                    mỗi catalog chỉ xuất hiện một lần. similarity là điểm giống tên, không phải xác suất AI nhận diện đúng.
                    Fuzzy không tự gán nguyên liệu.
                    """
    )
    public ApiResponse<List<IngredientCatalogCandidate>> searchCatalogIngredients(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(lookupService.search(keyword, limit));
    }

    @PostMapping
    @Operation(
            summary = "Tìm recipe từ nguyên liệu đã chọn",
            description = """
                    Chỉ CUSTOMER được gọi. Không gọi AI.
                    catalogIngredientIds gồm 1 đến 20 UUID nguyên liệu chuẩn, lấy từ
                    GET /catalog-ingredients (gõ tay) hoặc kết quả /images/recognize (ảnh).
                    ID trùng được gộp; ID không tồn tại hoặc inactive trả 400.
                    limit có thể bỏ qua, mặc định 3, tối đa 5.
                    Recipe xếp theo coverage = số loại nguyên liệu có / tổng loại nguyên liệu của recipe;
                    không so số lượng.
                    """
    )
    public ApiResponse<RecipeSuggestionResponse> suggest(
            @Valid @RequestBody RecipeSuggestionRequest request
    ) {
        return ApiResponse.success(
                recipeSuggestionService.suggest(request.catalogIngredientIds(), request.limit())
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
                Mỗi customer được nhận diện tối đa 5 ảnh/ngày (reset 0h giờ Việt Nam);
                hết lượt trả 429. remainingDailyQuota là số lượt còn lại trong ngày.
                Ảnh lỗi định dạng hoặc lỗi kết nối tới AI không bị trừ lượt.
                Mỗi lần nhận diện tốn điểm thưởng (giá xem ở GET /api/v1/customers/me/loyalty).
                Không đủ điểm trả 402 và không bị trừ lượt. AI lỗi, quá tải hoặc quá thời gian thì điểm được hoàn;
                AI từ chối ảnh hoặc không nhận ra nguyên liệu nào thì không hoàn.
                pointsCharged là số điểm vừa trừ, pointsBalance là số dư còn lại.
                Ảnh không được lưu lên R2.
                Kết quả có thể chưa chính xác; Customer kiểm tra, bỏ món nhầm,
                chọn lại món chưa matched (qua candidates hoặc GET /catalog-ingredients)
                rồi gửi các catalogIngredientId sang POST /api/v1/recipe-suggestions.
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
}
