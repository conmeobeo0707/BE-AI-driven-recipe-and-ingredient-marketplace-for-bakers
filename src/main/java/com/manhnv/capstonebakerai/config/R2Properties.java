package com.manhnv.capstonebakerai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.r2")
public record R2Properties(
        URI endpoint,
        String accessKeyId,
        String secretAccessKey,
        String publicBucket,
        String privateBucket,
        String publicUrl,
        Duration presignedUrlDuration
) {
}
