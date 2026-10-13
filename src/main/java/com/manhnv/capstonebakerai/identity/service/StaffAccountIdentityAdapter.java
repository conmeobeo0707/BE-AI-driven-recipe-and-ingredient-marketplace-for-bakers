package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.identity.repository.RefreshTokenRepository;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.port.StaffAccountIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class StaffAccountIdentityAdapter implements StaffAccountIdentityPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void initializeCredentials(User user) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String unsharedRandomPassword = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
        user.setPassword(passwordEncoder.encode(unsharedRandomPassword));
    }

    @Override
    public void revokeActiveSessions(User user, LocalDateTime revokedAt) {
        refreshTokenRepository.revokeAllActiveByUser(user, revokedAt);
    }
}
