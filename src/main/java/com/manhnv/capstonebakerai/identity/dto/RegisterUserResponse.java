package com.manhnv.capstonebakerai.identity.dto;

import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        String phone,
        UserStatus status,
        String address,
        String dateOfBirth,
        String gender,
        LocalDateTime createdAt
) {
}
