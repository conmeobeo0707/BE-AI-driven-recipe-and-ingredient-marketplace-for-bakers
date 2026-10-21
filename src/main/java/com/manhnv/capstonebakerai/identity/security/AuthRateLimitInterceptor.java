package com.manhnv.capstonebakerai.identity.security;

import tools.jackson.databind.ObjectMapper;
import com.manhnv.capstonebakerai.common.response.ApiResponse;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    private final AuthRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    private record Policy(int capacity, Duration window) {}

    private static final Map<String, Policy> POLICIES = Map.of(
            "/api/v1/auth/login", new Policy(10, Duration.ofMinutes(5)),
            "/api/v1/auth/register", new Policy(5, Duration.ofHours(1)),
            "/api/v1/auth/verify-email", new Policy(10, Duration.ofMinutes(15)),
            "/api/v1/auth/resend-verification", new Policy(3, Duration.ofHours(1)),
            "/api/v1/auth/forgot-password", new Policy(3, Duration.ofHours(1)),
            "/api/v1/auth/forgot-password/verify", new Policy(10, Duration.ofMinutes(15)),
            "/api/v1/auth/reset-password", new Policy(5, Duration.ofMinutes(15)),
            "/api/v1/auth/refresh", new Policy(30, Duration.ofMinutes(1)),
            "/api/v1/auth/logout", new Policy(30, Duration.ofMinutes(1))
    );

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Policy policy = POLICIES.get(request.getRequestURI());
        if (policy == null) {
            return true;
        }

        String ip = request.getRemoteAddr();
        String bucketKey = request.getRequestURI() + ":" + ip;

        ConsumptionProbe probe = rateLimiter.consume(
                bucketKey,
                policy.capacity(),
                policy.window()
        );

        if (probe.isConsumed()) {
            return true;
        }

        long retryAfterSeconds = Math.max(
                1,
                (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error("Too many requests. Please try again later.")
        );

        return false;
    }
}
