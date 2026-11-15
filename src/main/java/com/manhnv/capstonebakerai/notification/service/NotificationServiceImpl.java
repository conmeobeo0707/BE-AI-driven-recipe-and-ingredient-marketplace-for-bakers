package com.manhnv.capstonebakerai.notification.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.notification.dto.response.MarkAllReadResponse;
import com.manhnv.capstonebakerai.notification.dto.response.NotificationResponse;
import com.manhnv.capstonebakerai.notification.dto.response.UnreadCountResponse;
import com.manhnv.capstonebakerai.notification.entity.Notification;
import com.manhnv.capstonebakerai.notification.enums.NotificationRefType;
import com.manhnv.capstonebakerai.notification.exception.NotificationErrorCode;
import com.manhnv.capstonebakerai.notification.mapper.NotificationMapper;
import com.manhnv.capstonebakerai.notification.repository.NotificationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {

    NotificationRepository repository;
    NotificationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAll(UUID userId, Boolean read, Pageable pageable) {
        Page<Notification> page = (read == null)
                ? repository.findByUserId(userId, pageable)
                : repository.findByUserIdAndRead(userId, read, pageable);
        return page.map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID userId, UUID id) {
        return mapper.toResponse(findOrThrow(userId, id));
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID id) {
        Notification notification = findOrThrow(userId, id);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = repository.save(notification);
        }
        return mapper.toResponse(notification);
    }

    @Override
    @Transactional
    public MarkAllReadResponse markAllAsRead(UUID userId) {
        return new MarkAllReadResponse(repository.markAllAsRead(userId, LocalDateTime.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(UUID userId) {
        return new UnreadCountResponse(repository.countByUserIdAndReadFalse(userId));
    }

    @Override
    public void create(UUID userId, String title, String content, NotificationRefType refType, UUID refId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .refType(refType)
                .refId(refId)
                .build();
        repository.save(notification);
    }

    @Override
    public void createForUsers(Collection<UUID> userIds, String title, String content, NotificationRefType refType, UUID refId) {
        List<Notification> list = userIds.stream()
                .map(uid -> Notification.builder()
                        .userId(uid)
                        .title(title)
                        .content(content)
                        .refType(refType)
                        .refId(refId)
                        .build())
                .toList();
        repository.saveAll(list);
    }

    private Notification findOrThrow(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new AppException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
