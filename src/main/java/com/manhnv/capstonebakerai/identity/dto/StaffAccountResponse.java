package com.manhnv.capstonebakerai.identity.dto;

import com.manhnv.capstonebakerai.identity.enums.Role;
import com.manhnv.capstonebakerai.identity.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffAccountResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        UserStatus status,
        String phone,
        String address,
        String dateOfBirth,
        String gender,
        String avatarUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
