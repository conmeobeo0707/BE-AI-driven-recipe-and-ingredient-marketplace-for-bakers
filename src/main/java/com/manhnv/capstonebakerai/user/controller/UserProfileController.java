package com.manhnv.capstonebakerai.user.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.user.dto.UpdateUserProfileRequest;
import com.manhnv.capstonebakerai.user.dto.UserProfileResponse;
import com.manhnv.capstonebakerai.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "UserProfileController", description = "Đọc và cập nhật hồ sơ của tài khoản đang đăng nhập. Cần access token Bearer.")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "Lấy hồ sơ tài khoản hiện tại", description = "Trả về hồ sơ User dựa trên access token, không nhận userId từ client. phone, address, dateOfBirth, gender và avatarUrl có thể null nếu chưa được khai báo.")
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.success(userProfileService.getProfile(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping
    @Operation(summary = "Cập nhật hồ sơ tài khoản hiện tại", description = "Cập nhật fullName, phone, address, dateOfBirth và gender. fullName bắt buộc; các field còn lại có thể null hoặc bỏ khỏi body để xóa giá trị hiện tại. Email, role và status không được cập nhật qua API này.")
    public ApiResponse<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.success(userProfileService.updateProfile(SecurityUtils.getCurrentUserId(), request));
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Tải avatar lên hoặc thay avatar", description = "Gửi multipart/form-data với field file. Hỗ trợ JPEG, PNG, WebP, tối đa 5 MB. File được backend xác thực và tải lên R2; avatar cũ được dọn sau khi cập nhật thành công.")
    public ApiResponse<UserProfileResponse> uploadAvatar(
            @Parameter(description = "Tệp ảnh JPEG, PNG hoặc WebP; bắt buộc, tối đa 5 MB", required = true)
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userProfileService.uploadAvatar(SecurityUtils.getCurrentUserId(), file));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Xóa avatar", description = "Xóa URL avatar khỏi hồ sơ hiện tại và dọn object trên R2 sau khi cập nhật database thành công. Các field hồ sơ khác không đổi.")
    public ApiResponse<UserProfileResponse> deleteAvatar() {
        return ApiResponse.success(userProfileService.deleteAvatar(SecurityUtils.getCurrentUserId()));
    }
}
