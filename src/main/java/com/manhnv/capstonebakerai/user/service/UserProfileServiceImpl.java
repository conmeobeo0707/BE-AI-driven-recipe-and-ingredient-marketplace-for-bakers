package com.manhnv.capstonebakerai.user.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.media.enums.R2BucketType;
import com.manhnv.capstonebakerai.media.service.R2StorageService;
import com.manhnv.capstonebakerai.user.dto.UpdateUserProfileRequest;
import com.manhnv.capstonebakerai.user.dto.UserProfileResponse;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.exception.UserErrorCode;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    //max 5mb cho avatar
    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;

    private final UserRepository userRepository;
    private final R2StorageService r2StorageService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateUserProfileRequest request) {
        User user = findUser(userId);

        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setAddress(request.address());
        user.setBod(request.dateOfBirth());
        user.setGender(request.gender());
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserProfileResponse uploadAvatar(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(UserErrorCode.AVATAR_REQUIRED);
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new AppException(UserErrorCode.AVATAR_TOO_LARGE);
        }

        String contentType = file.getContentType();
        String extension = extensionFor(contentType);
        if (extension == null) {
            throw new AppException(UserErrorCode.INVALID_AVATAR_TYPE);
        }

        User user = findUser(userId);
        String oldObjectKey = ownedAvatarKey(user);
        String newObjectKey = "avatars/users/" + userId + "/" + UUID.randomUUID() + "." + extension;

        try {
            r2StorageService.upload(R2BucketType.PUBLIC, newObjectKey, file.getBytes(), contentType);
        } catch (IOException | RuntimeException exception) {
            throw new AppException(UserErrorCode.AVATAR_STORAGE_FAILED);
        }

        try {
            user.setAvatarUrl(r2StorageService.publicUrl(newObjectKey));
            user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            User savedUser = userRepository.saveAndFlush(user);
            registerAvatarCleanup(newObjectKey, oldObjectKey, userId);
            return toResponse(savedUser);
        } catch (RuntimeException exception) {
            deleteAvatarQuietly(newObjectKey, userId);
            throw exception;
        }
    }

    @Override
    @Transactional
    public UserProfileResponse deleteAvatar(UUID userId) {
        User user = findUser(userId);
        String oldObjectKey = ownedAvatarKey(user);

        user.setAvatarUrl(null);
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        User savedUser = userRepository.saveAndFlush(user);
        registerAvatarCleanup(null, oldObjectKey, userId);
        return toResponse(savedUser);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getPhone(),
                user.getAddress(),
                user.getBod(),
                user.getGender(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String extensionFor(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> null;
        };
    }

    private String ownedAvatarKey(User user) {
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }

        String publicPrefix;
        try {
            publicPrefix = r2StorageService.publicUrl("");
        } catch (RuntimeException exception) {
            log.warn("Could not resolve public R2 URL while replacing avatar for user {}", user.getUserId(), exception);
            return null;
        }

        if (!avatarUrl.startsWith(publicPrefix)) {
            return null;
        }
        String objectKey = avatarUrl.substring(publicPrefix.length());
        if (!objectKey.startsWith("avatars/users/" + user.getUserId() + "/")) {
            return null;
        }
        return objectKey;
    }

    private void registerAvatarCleanup(String newObjectKey, String oldObjectKey, UUID userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            if (oldObjectKey != null) {
                deleteAvatarQuietly(oldObjectKey, userId);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (oldObjectKey != null) {
                    deleteAvatarQuietly(oldObjectKey, userId);
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED && newObjectKey != null) {
                    deleteAvatarQuietly(newObjectKey, userId);
                }
            }
        });
    }

    private void deleteAvatarQuietly(String objectKey, UUID userId) {
        try {
            r2StorageService.delete(R2BucketType.PUBLIC, objectKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to clean up avatar object for user {}", userId, exception);
        }
    }
}
