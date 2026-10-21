package com.manhnv.capstonebakerai.identity.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.identity.dto.AuthenticationResult;
import com.manhnv.capstonebakerai.identity.dto.LoginRequest;
import com.manhnv.capstonebakerai.identity.dto.LoginResponse;
import com.manhnv.capstonebakerai.identity.dto.MessageResponse;
import com.manhnv.capstonebakerai.identity.entity.RefreshToken;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import com.manhnv.capstonebakerai.identity.exception.IdentityErrorCode;
import com.manhnv.capstonebakerai.identity.repository.RefreshTokenRepository;
import com.manhnv.capstonebakerai.identity.security.AuthRateLimiter;
import com.manhnv.capstonebakerai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService{

    private final static SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final CustomerRepository customerRepository;
    private final AuthRateLimiter authRateLimiter;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.access-token-ttl}")
    private Duration accessTokenTtl;

    @Value("${app.jwt.refresh-token-ttl}")
    private Duration refreshTokenTtl;

    @Override
    @Transactional
    public AuthenticationResult login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            throw invalidCredentialsOrRateLimited(email);
        }

        if(!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentialsOrRateLimited(email);
        }
        checkUserCanLogin(user);
        return issueTokenPair(user, null);
    }

    @Override
    @Transactional
    public AuthenticationResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
        }
        String tokenHash  = hashToken(rawRefreshToken);

        RefreshToken oldToken = refreshTokenRepository.
                findByTokenHashForUpdate(tokenHash)
                .filter(token -> token.getRevokedAt() == null)
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC)))
                .orElseThrow(() -> new AppException(IdentityErrorCode.INVALID_REFRESH_TOKEN));

        User user = oldToken.getUser();
        checkUserCanLogin(user);

        oldToken.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        return issueTokenPair(user, oldToken);
    }

    @Override
    @Transactional
    public MessageResponse logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException(IdentityErrorCode.INVALID_REFRESH_TOKEN);
        }
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(tokenHash).orElse(null);
        LocalDateTime revokedAt = LocalDateTime.now(ZoneOffset.UTC);
        // Always lock from ancestor to descendant. A concurrent rotation either finishes
        // before this lock (and is followed), or sees the revocation and cannot refresh.
        while (token != null) {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(revokedAt);
            }
            token = token.getReplacedById() == null ? null
                    : refreshTokenRepository.findByIdForUpdate(token.getReplacedById())
                            .orElseThrow(() -> new IllegalStateException("Broken refresh token rotation chain"));
        }

        return new MessageResponse("Logged out successfully");
    }

    private void checkUserCanLogin(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(IdentityErrorCode.ACCOUNT_DISABLED);
        }

        if (user.getRole() == Role.CUSTOMER) {
            Customer customer = customerRepository.findByUserId(user.getUserId())
                    .orElseThrow(() -> new AppException(
                            IdentityErrorCode.EMAIL_NOT_VERIFIED
                    ));

            if (!customer.isConfirmed()) {
                throw new AppException(IdentityErrorCode.EMAIL_NOT_VERIFIED);
            }
        }
    }

    private AppException invalidCredentialsOrRateLimited(String email) {
        if (!authRateLimiter.consumeForEmail(
                "login",
                email,
                5,
                Duration.ofMinutes(15)
        ).isConsumed()) {
            return new AppException(IdentityErrorCode.RATE_LIMIT_EXCEEDED);
        }
        return new AppException(IdentityErrorCode.INVALID_CREDENTIALS);
    }

    private AuthenticationResult issueTokenPair(User user, RefreshToken previousToken) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getUserId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("role", user.getRole().name())
                .build();

        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();

        String rawRefreshToken = createRefreshToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawRefreshToken));
        refreshToken.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(refreshTokenTtl));
        // Insert the successor before updating the predecessor's foreign key.
        refreshTokenRepository.saveAndFlush(refreshToken);
        if (previousToken != null) {
            previousToken.setReplacedById(refreshToken.getRefreshTokenId());
        }

        LoginResponse response = new LoginResponse(
                accessToken,
                "Bearer",
                accessTokenTtl.toSeconds(),
                user.getUserId(),
                user.getEmail(),
                user.getRole()
        );
        return new AuthenticationResult(response, rawRefreshToken);
    }

    private String createRefreshToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
