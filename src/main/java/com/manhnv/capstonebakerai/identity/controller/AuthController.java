package com.manhnv.capstonebakerai.identity.controller;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.identity.dto.*;
import com.manhnv.capstonebakerai.identity.exception.IdentityErrorCode;
import com.manhnv.capstonebakerai.identity.service.AuthenticationService;
import com.manhnv.capstonebakerai.identity.service.EmailVerificationService;
import com.manhnv.capstonebakerai.identity.service.PasswordResetService;
import com.manhnv.capstonebakerai.identity.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AuthController", description = "Đăng ký, xác minh email, đăng nhập và quản lý phiên. Các endpoint không yêu cầu access token; refresh cần refresh cookie, logout nhận cookie nếu có. Cả refresh và logout yêu cầu X-Auth-Request: 1.")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

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
    @Operation(summary = "Đăng ký tài khoản Customer", description = "Tạo User với role CUSTOMER và hồ sơ Customer, sau đó gửi mã xác minh 6 chữ số về email. fullName, email, password bắt buộc; phone, address, dateOfBirth, gender có thể null hoặc bỏ qua. avatarUrl được để trống khi đăng ký.")
    public ApiResponse<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ApiResponse.success(registrationService.register(request));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Xác minh email", description = "Gửi email và mã 6 chữ số. Nếu mã đúng và còn hạn, Customer được đánh dấu đã xác minh. Gọi lại với email đã xác minh trả HTTP 200 cùng message thông báo email đã xác minh.")
    public ApiResponse<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.success(emailVerificationService.verify(request.email(), request.code()));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Gửi lại mã xác minh email", description = "Nhận email; nếu account tồn tại và chưa xác minh, hệ thống gửi mã mới. Response luôn chung để không tiết lộ email có đăng ký hay không. Code có thời hạn theo cấu hình hệ thống.")
    public ApiResponse<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ApiResponse.success(emailVerificationService.resend(request.email()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu mã đặt lại mật khẩu", description = "Nhận email và yêu cầu mã xác minh 6 chữ số. Response không tiết lộ email có tài khoản hay không. Mã được gửi khi tài khoản tồn tại, đang ACTIVE và qua thời gian cooldown.")
    public ApiResponse<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.success(passwordResetService.requestCode(request.email()));
    }

    @PostMapping("/forgot-password/verify")
    @Operation(summary = "Kiểm tra mã đặt lại mật khẩu", description = "Bước kiểm tra mã trước khi hiển thị form đổi mật khẩu. Body cần email và code 6 chữ số. Mã hợp lệ vẫn phải gửi lại ở endpoint /reset-password cùng newPassword trước khi hết hạn.")
    public ApiResponse<MessageResponse> verifyPasswordResetCode(
            @Valid @RequestBody VerifyPasswordResetCodeRequest request) {
        return ApiResponse.success(passwordResetService.verifyCode(request.email(), request.code()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt mật khẩu mới", description = "Nhận email, code 6 chữ số và newPassword. Khi thành công, mã bị xóa, các refresh session đang hoạt động bị thu hồi và cookie refresh_token được xóa.")
    public ApiResponse<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request, HttpServletResponse response) {
        MessageResponse result = passwordResetService.resetPassword(
                request.email(), request.code(), request.newPassword());
        clearRefreshCookie(response);
        return ApiResponse.success(result);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Nhận email và password. Response data chứa access token; refresh token được trả trong HttpOnly cookie refresh_token, không nằm trong JSON body.")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthenticationResult result = authenticationService.login(request);
        setRefreshCookie(response, result.refreshToken());
        return ApiResponse.success(result.response());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token", description = "Đọc refresh_token từ HttpOnly cookie và xoay token: cookie mới được trả trong Set-Cookie, JSON data chứa access token mới. Gửi header X-Auth-Request: 1. Frontend cần gửi request kèm credentials để trình duyệt gửi cookie.")
    public ApiResponse<LoginResponse> refresh(
            @Parameter(description = "HttpOnly refresh cookie do API login/refresh thiết lập", in = ParameterIn.COOKIE, name = REFRESH_COOKIE_NAME)
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @Parameter(description = "Header chống request giả mạo qua browser; giá trị bắt buộc là 1", in = ParameterIn.HEADER, name = "X-Auth-Request", required = true, example = "1")
            @RequestHeader(name = "X-Auth-Request", required = false) String requestMarker,
            HttpServletResponse response
    ) {
        requireAuthRequest(requestMarker);
        AuthenticationResult result = authenticationService.refresh(refreshToken);
        setRefreshCookie(response, result.refreshToken());
        return ApiResponse.success(result.response());
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất", description = "Gửi X-Auth-Request: 1. Nếu có refresh_token cookie thì session tương ứng bị revoke; sau đó cookie được xóa. Nếu cookie không có, API vẫn trả thành công và xóa cookie phía client.")
    public ApiResponse<MessageResponse> logout(
            @Parameter(description = "HttpOnly refresh cookie cần revoke; có thể vắng mặt", in = ParameterIn.COOKIE, name = REFRESH_COOKIE_NAME)
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @Parameter(description = "Header chống request giả mạo qua browser; giá trị bắt buộc là 1", in = ParameterIn.HEADER, name = "X-Auth-Request", required = true, example = "1")
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
