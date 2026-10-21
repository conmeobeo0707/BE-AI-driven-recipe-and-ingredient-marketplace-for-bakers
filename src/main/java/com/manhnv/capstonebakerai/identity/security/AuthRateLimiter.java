package com.manhnv.capstonebakerai.identity.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class AuthRateLimiter {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();

    public ConsumptionProbe consume(String key, int capacity, Duration refillWindow) {
        Bucket bucket = buckets.get(key, ignored ->
                Bucket.builder()
                        .addLimit(limit -> limit
                                .capacity(capacity)
                                .refillGreedy(capacity, refillWindow))
                        .build()
        );

        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe consumeForEmail(
            String action,
            String email,
            int capacity,
            Duration refillWindow
    ) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return consume(action + ":email:" + hash(normalizedEmail), capacity, refillWindow);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
