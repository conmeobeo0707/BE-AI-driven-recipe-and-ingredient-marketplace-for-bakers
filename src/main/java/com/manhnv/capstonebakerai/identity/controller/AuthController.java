package com.manhnv.capstonebakerai.identity.controller;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.identity.dto.*;
import com.manhnv.capstonebakerai.identity.exception.IdentityErrorCode;
import com.manhnv.capstonebakerai.identity.service.AuthenticationService;
import com.manhnv.capstonebakerai.identity.service.EmailVerificationService;
import com.manhnv.capstonebakerai.identity.service.RegistrationService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationService authenticationService;

    @Value("${app.jwt.refresh-token-ttl}")
    private Duration refreshTokenTtl;

    @Value("${app.auth.refresh-cookie-secure}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie-same-site}")
    private String refreshCookieSameSite;

    @PostConstruct
    void validateCookieSettings() {
        if ("None".equalsIgnoreCase(refreshCookieSameSite) && !refreshCookieSecure) {
            throw new IllegalStateException("SameSite=None requires REFRESH_COOKIE_SECURE=true");
        }
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ApiResponse.success(registrationService.register(request));
    }

    @PostMapping("/verify-email")
    public ApiResponse<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(emailVerificationService.verify(request.email(), request.code()));
    }

    @PostMapping("/resend-verification")
    public ApiResponse<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ApiResponse.success(emailVerificationService.resend(request.email()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthenticationResult result = authenticationService.login(request);
        setRefreshCookie(response, result.refreshToken());
        return ApiResponse.success(result.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(name = "X-Auth-Request", required = false) String requestMarker,
            HttpServletResponse response
    ) {
        requireAuthRequest(requestMarker);
        AuthenticationResult result = authenticationService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return ApiResponse.success(result.response());
    }

    @PostMapping("/logout")
    public ApiResponse<MessageResponse> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(name = "X-Auth-Request", required = false) String requestMarker,
            HttpServletResponse response
    ) {
        requireAuthRequest(requestMarker);
        MessageResponse result = refreshToken == null || refreshToken.isBlank()
                ? new MessageResponse("Logged out successfully")
                : authenticationService.logout(refreshToken);
        clearRefreshCookie(response);
        return ApiResponse.success(result);
    }

    private void requireAuthRequest(String requestMarker) {
        if (!"1".equals(requestMarker)) {
            throw new AppException(IdentityErrorCode.INVALID_AUTH_REQUEST);
        }
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        writeRefreshCookie(response, token, refreshTokenTtl);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        writeRefreshCookie(response, "", Duration.ZERO);
    }

    private void writeRefreshCookie(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
