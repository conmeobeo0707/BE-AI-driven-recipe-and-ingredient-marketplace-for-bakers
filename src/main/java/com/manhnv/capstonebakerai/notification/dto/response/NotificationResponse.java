package com.manhnv.capstonebakerai.notification.dto.response;

import com.manhnv.capstonebakerai.notification.enums.NotificationRefType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String content,
        NotificationRefType refType,
        UUID refId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt) {
}
