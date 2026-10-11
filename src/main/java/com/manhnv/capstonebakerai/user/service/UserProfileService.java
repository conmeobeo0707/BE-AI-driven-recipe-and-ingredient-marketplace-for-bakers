package com.manhnv.capstonebakerai.user.service;

import com.manhnv.capstonebakerai.user.dto.UpdateUserProfileRequest;
import com.manhnv.capstonebakerai.user.dto.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserProfileService {
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request);
    UserProfileResponse uploadAvatar(UUID userId, MultipartFile file);
    UserProfileResponse deleteAvatar(UUID userId);
}
