package com.manhnv.capstonebakerai.hobby.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import com.manhnv.capstonebakerai.hobby.dto.SaveHobbyRequest;
import com.manhnv.capstonebakerai.hobby.dto.UpdateHobbyActiveRequest;
import com.manhnv.capstonebakerai.hobby.service.HobbyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/hobbies")
@RequiredArgsConstructor
@Tag(name = "AdminHobbyController", description = "CRUD hobby dành cho ADMIN. Xóa hobby là soft delete (active=false), không xóa vĩnh viễn khỏi database.")
public class AdminHobbyController {

    private final HobbyService hobbyService;

    @GetMapping
    @Operation(summary = "Lấy tất cả hobby", description = "Trả về cả hobby active và inactive, sắp xếp theo tên tăng dần.")
    public ApiResponse<List<HobbyResponse>> getAll() {
        return ApiResponse.success(hobbyService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy hobby theo ID", description = "ID là UUID. Có thể lấy cả hobby đang inactive.")
    public ApiResponse<HobbyResponse> getById(@Parameter(description = "UUID của hobby", required = true) @PathVariable UUID id) {
        return ApiResponse.success(hobbyService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo hobby", description = "name bắt buộc và tối đa 100 ký tự; description tùy chọn, tối đa 255 ký tự và có thể null. Hobby mới mặc định active=true. Tên không phân biệt hoa thường và không được trùng.")
    public ApiResponse<HobbyResponse> create(@Valid @RequestBody SaveHobbyRequest request) {
        return ApiResponse.success(hobbyService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật hobby", description = "Thay name và description theo request body. name bắt buộc; description có thể null để xóa mô tả. Trạng thái active không đổi qua API này.")
    public ApiResponse<HobbyResponse> update(
            @Parameter(description = "UUID của hobby cần cập nhật", required = true) @PathVariable UUID id,
            @Valid @RequestBody SaveHobbyRequest request
    ) {
        return ApiResponse.success(hobbyService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Bật hoặc tắt hobby", description = "Body cần active=true hoặc false. Hobby inactive không xuất hiện trong danh sách public và không thể được Customer chọn.")
    public ApiResponse<HobbyResponse> setActive(
            @Parameter(description = "UUID của hobby", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateHobbyActiveRequest request
    ) {
        return ApiResponse.success(hobbyService.setActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Ngừng sử dụng hobby", description = "Soft delete: đặt active=false và trả hobby sau khi cập nhật. Lịch sử liên kết sở thích của Customer không bị xóa.")
    public ApiResponse<HobbyResponse> deactivate(@Parameter(description = "UUID của hobby", required = true) @PathVariable UUID id) {
        return ApiResponse.success(hobbyService.setActive(id, false));
    }
}
