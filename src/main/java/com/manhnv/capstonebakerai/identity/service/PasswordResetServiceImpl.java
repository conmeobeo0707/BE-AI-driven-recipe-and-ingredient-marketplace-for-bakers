package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.identity.dto.MessageResponse;
import com.manhnv.capstonebakerai.identity.entity.VerificationCode;
import com.manhnv.capstonebakerai.identity.entity.User;
import com.manhnv.capstonebakerai.identity.enums.VerificationPurpose;
import com.manhnv.capstonebakerai.identity.event.PasswordResetRequestedEvent;
import com.manhnv.capstonebakerai.identity.exception.IdentityErrorCode;
import com.manhnv.capstonebakerai.identity.repository.VerificationCodeRepository;
import com.manhnv.capstonebakerai.identity.repository.RefreshTokenRepository;
import com.manhnv.capstonebakerai.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final MessageResponse REQUEST_RESPONSE = new MessageResponse(
            "If an active account exists, a password reset code has been sent");

    private final UserRepository userRepository;
    private final VerificationCodeRepository codeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.password-reset.code-expiration}")
    private Duration codeExpiration;

    @Value("${app.password-reset.resend-cooldown}")
    private Duration resendCooldown;

    @Override
    @Transactional
    public MessageResponse requestCode(String email) {
        User user = userRepository.findByEmailIgnoreCaseForUpdate(normalize(email)).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return REQUEST_RESPONSE;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        VerificationCode resetCode = codeRepository
                .findByUserUserIdAndPurpose(user.getUserId(), VerificationPurpose.PASSWORD_RESET)
                .orElseGet(VerificationCode::new);
        if (resetCode.getCreatedAt() != null && resetCode.getCreatedAt().plus(resendCooldown).isAfter(now)) {
            return REQUEST_RESPONSE;
        }

        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        resetCode.setUser(user);
        resetCode.setPurpose(VerificationPurpose.PASSWORD_RESET);
        resetCode.setCodeHash(passwordEncoder.encode(code));
        resetCode.setCreatedAt(now);
        resetCode.setExpiresAt(now.plus(codeExpiration));
        codeRepository.save(resetCode);
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getUserId(), user.getEmail(), code));
        return REQUEST_RESPONSE;
    }

    @Override
    @Transactional
    public MessageResponse verifyCode(String email, String code) {
        User user = userRepository.findByEmailIgnoreCase(normalize(email))
                .filter(account -> Boolean.TRUE.equals(account.getIsActive()))
                .orElseThrow(this::invalidCode);
        VerificationCode resetCode = codeRepository
                .findByUserIdAndPurposeForUpdate(user.getUserId(), VerificationPurpose.PASSWORD_RESET)
                .orElseThrow(this::invalidCode);
        checkCode(resetCode, code);
        return new MessageResponse("Code verified. Submit the code again with a new password");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(String email, String code, String newPassword) {
        User user = userRepository.findByEmailIgnoreCaseForUpdate(normalize(email))
                .filter(account -> Boolean.TRUE.equals(account.getIsActive()))
                .orElseThrow(this::invalidCode);
        VerificationCode resetCode = codeRepository
                .findByUserIdAndPurposeForUpdate(user.getUserId(), VerificationPurpose.PASSWORD_RESET)
                .orElseThrow(this::invalidCode);
        checkCode(resetCode, code);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(now);
        codeRepository.delete(resetCode);
        refreshTokenRepository.revokeAllActiveByUser(user, now);
        return new MessageResponse("Password reset successfully. Please log in again");
    }

    private void checkCode(VerificationCode resetCode, String code) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!resetCode.getExpiresAt().isAfter(now)) {
            throw invalidCode();
        }
        if (!passwordEncoder.matches(code, resetCode.getCodeHash())) {
            throw invalidCode();
        }
    }

    private AppException invalidCode() {
        return new AppException(IdentityErrorCode.INVALID_PASSWORD_RESET_CODE);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
