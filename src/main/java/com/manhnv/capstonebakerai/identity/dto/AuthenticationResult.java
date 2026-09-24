package com.manhnv.capstonebakerai.identity.dto;

public record AuthenticationResult(
        LoginResponse response,
        String refreshToken
) {
}
