package com.manhnv.capstonebakerai.hobby.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.hobby.dto.HobbyResponse;
import com.manhnv.capstonebakerai.hobby.service.HobbyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hobbies")
@RequiredArgsConstructor
@Tag(name = "HobbyController", description = "Danh sách hobby active để giao diện hiển thị cho Customer và Admin đã đăng nhập.")
public class HobbyController {

    private final HobbyService hobbyService;

    @GetMapping
    @Operation(summary = "Lấy danh sách hobby đang hoạt động", description = "Chỉ trả về hobby active=true, sắp xếp theo tên tăng dần. API yêu cầu access token; chỉ CUSTOMER hoặc ADMIN được gọi.")
    public ApiResponse<List<HobbyResponse>> getActiveHobbies() {
        return ApiResponse.success(hobbyService.getActiveHobbies());
    }
}
