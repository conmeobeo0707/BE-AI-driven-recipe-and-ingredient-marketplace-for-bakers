package com.manhnv.capstonebakerai.user.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.user.dto.UpdateUserProfileRequest;
import com.manhnv.capstonebakerai.user.dto.UserProfileResponse;
import com.manhnv.capstonebakerai.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.success(userProfileService.getProfile(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.success(userProfileService.updateProfile(SecurityUtils.getCurrentUserId(), request));
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UserProfileResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(userProfileService.uploadAvatar(SecurityUtils.getCurrentUserId(), file));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<UserProfileResponse> deleteAvatar() {
        return ApiResponse.success(userProfileService.deleteAvatar(SecurityUtils.getCurrentUserId()));
    }
}
