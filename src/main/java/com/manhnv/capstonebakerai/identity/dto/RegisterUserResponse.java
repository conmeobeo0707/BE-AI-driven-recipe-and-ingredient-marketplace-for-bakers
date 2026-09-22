package com.manhnv.capstonebakerai.identity.dto;

import com.manhnv.capstonebakerai.identity.enums.Role;
import com.manhnv.capstonebakerai.identity.enums.SkillLevel;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegisterUserResponse(
        UUID userId,
        String name,
        String email,
        Role role,
        String phone,
        Boolean isActive,
        Boolean isConfirmed,
        String avatarUrl,
        String address,
        String bod,
        String gender,
        SkillLevel skillLevel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
