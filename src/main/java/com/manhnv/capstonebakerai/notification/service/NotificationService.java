package com.manhnv.capstonebakerai.notification.service;

import com.manhnv.capstonebakerai.notification.dto.response.MarkAllReadResponse;
import com.manhnv.capstonebakerai.notification.dto.response.NotificationResponse;
import com.manhnv.capstonebakerai.notification.dto.response.UnreadCountResponse;
import com.manhnv.capstonebakerai.notification.enums.NotificationRefType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.UUID;

public interface NotificationService {
    Page<NotificationResponse> getAll(UUID userId, Boolean read, Pageable pageable);
    NotificationResponse getById(UUID userId, UUID id);
    NotificationResponse markAsRead(UUID userId, UUID id);
    MarkAllReadResponse markAllAsRead(UUID userId);
    UnreadCountResponse countUnread(UUID userId);
    void create(UUID userId, String title, String content, NotificationRefType refType, UUID refId);
    void createForUsers(Collection<UUID> userIds, String title, String content, NotificationRefType refType, UUID refId);

}
