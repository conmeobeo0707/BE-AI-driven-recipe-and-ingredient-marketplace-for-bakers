package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.identity.dto.MessageResponse;

import java.util.UUID;

public interface EmailVerificationService {
    void sendRegistrationCode(UUID userId);

    MessageResponse verify(String email, String code);

    MessageResponse resend(String email);
}
