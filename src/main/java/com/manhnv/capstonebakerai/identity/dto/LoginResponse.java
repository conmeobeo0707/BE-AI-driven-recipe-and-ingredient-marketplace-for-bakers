package com.manhnv.capstonebakerai.identity.dto;

import com.manhnv.capstonebakerai.identity.enums.Role;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        Role role
) {
}
