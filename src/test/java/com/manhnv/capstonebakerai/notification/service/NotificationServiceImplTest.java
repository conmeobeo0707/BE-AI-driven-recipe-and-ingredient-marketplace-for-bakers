package com.manhnv.capstonebakerai.notification.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.notification.dto.response.MarkAllReadResponse;
import com.manhnv.capstonebakerai.notification.dto.response.NotificationResponse;
import com.manhnv.capstonebakerai.notification.dto.response.UnreadCountResponse;
import com.manhnv.capstonebakerai.notification.entity.Notification;
import com.manhnv.capstonebakerai.notification.enums.NotificationRefType;
import com.manhnv.capstonebakerai.notification.mapper.NotificationMapper;
import com.manhnv.capstonebakerai.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

    @Mock
    NotificationRepository repository;
    @Mock
    NotificationMapper mapper;
    @InjectMocks
    NotificationServiceImpl notificationService;

    @Captor
    ArgumentCaptor<List<Notification>> listCaptor;

    // Get all
    // UTCID01
    @Test
    void getAll_readNull_shouldReturnAllNotifications() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Notification n = Notification.builder().userId(userId).title("t").build();
        NotificationResponse r = mock(NotificationResponse.class);

        when(repository.findByUserId(userId, pageable)).thenReturn(new PageImpl<>(List.of(n), pageable, 1));
        when(mapper.toResponse(n)).thenReturn(r);

        Page<NotificationResponse> result = notificationService.getAll(userId, null, pageable);

        assertThat(result.getContent()).containsExactly(r);
        verify(repository, never()).findByUserIdAndRead(any(), anyBoolean(), any());
    }

    // UTCID02
    @Test
    void getAll_readTrue_shouldReturnReadNotifications() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findByUserIdAndRead(userId, true, pageable)).thenReturn(Page.empty(pageable));

        Page<NotificationResponse> result = notificationService.getAll(userId, true, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(repository).findByUserIdAndRead(userId, true, pageable);
        verify(repository, never()).findByUserId(any(), any());
    }

    // UTCID03
    @Test
    void getAll_readFalse_shouldReturnUnreadNotifications() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Notification n = Notification.builder().userId(userId).title("t").build();
        NotificationResponse r = mock(NotificationResponse.class);

        when(repository.findByUserIdAndRead(userId, false, pageable)).thenReturn(new PageImpl<>(List.of(n), pageable, 1));
        when(mapper.toResponse(n)).thenReturn(r);

        Page<NotificationResponse> result = notificationService.getAll(userId, false, pageable);

        assertThat(result.getContent()).containsExactly(r);
        verify(repository).findByUserIdAndRead(userId, false, pageable);
    }

    // Get by id
    // UTCID01
    @Test
    void getById_ownNotification_shouldReturnResponse() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Notification n = Notification.builder().userId(userId).build();
        NotificationResponse expected = mock(NotificationResponse.class);

        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(n));
        when(mapper.toResponse(n)).thenReturn(expected);

        assertThat(notificationService.getById(userId, id)).isSameAs(expected);
    }

    // UTCID02
    @Test
    void getById_notFound_shouldThrow() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getById(userId, id))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(mapper);
    }

    // UTCID03
    @Test
    void getById_belongsToOtherUser_shouldThrow() {
        UUID otherUserId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        when(repository.findByIdAndUserId(id, otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getById(otherUserId, id))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(mapper);
    }

    // Mark as read
    // UTCID01
    @Test
    void markAsRead_unreadNotification_shouldMarkAndSave() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Notification n = Notification.builder().userId(userId).read(false).build();
        NotificationResponse expected = mock(NotificationResponse.class);

        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(n));
        when(repository.save(n)).thenReturn(n);
        when(mapper.toResponse(n)).thenReturn(expected);

        NotificationResponse response = notificationService.markAsRead(userId, id);

        assertThat(n.isRead()).isTrue();
        assertThat(n.getReadAt()).isNotNull();
        verify(repository).save(n);
        assertThat(response).isSameAs(expected);
    }

    // UTCID02
    @Test
    void markAsRead_alreadyRead_shouldNotSaveAgain() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        LocalDateTime oldReadAt = LocalDateTime.of(2026, 1, 1, 8, 0);
        Notification n = Notification.builder().userId(userId).read(true).readAt(oldReadAt).build();
        NotificationResponse expected = mock(NotificationResponse.class);

        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.of(n));
        when(mapper.toResponse(n)).thenReturn(expected);

        NotificationResponse response = notificationService.markAsRead(userId, id);

        assertThat(n.getReadAt()).isEqualTo(oldReadAt);
        verify(repository, never()).save(any());
        assertThat(response).isSameAs(expected);
    }

    // UTCID03
    @Test
    void markAsRead_notFound_shouldThrow() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        when(repository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(userId, id))
                .isInstanceOf(AppException.class);
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    // UTCID04
    @Test
    void markAsRead_belongsToOtherUser_shouldThrow() {
        UUID otherUserId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        when(repository.findByIdAndUserId(id, otherUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(otherUserId, id))
                .isInstanceOf(AppException.class);
        verify(repository, never()).save(any());
    }

    // Mark all as read
    // UTCID01
    @Test
    void markAllAsRead_hasUnread_shouldReturnUpdatedCount() {
        UUID userId = UUID.randomUUID();

        when(repository.markAllAsRead(eq(userId), any(LocalDateTime.class))).thenReturn(5);

        MarkAllReadResponse response = notificationService.markAllAsRead(userId);

        assertThat(response).isEqualTo(new MarkAllReadResponse(5));
        verify(repository).markAllAsRead(eq(userId), any(LocalDateTime.class));
    }

    // UTCID02
    @Test
    void markAllAsRead_noUnread_shouldReturnZero() {
        UUID userId = UUID.randomUUID();

        when(repository.markAllAsRead(eq(userId), any(LocalDateTime.class))).thenReturn(0);

        MarkAllReadResponse response = notificationService.markAllAsRead(userId);

        assertThat(response).isEqualTo(new MarkAllReadResponse(0));
    }

    // Count unread
    // UTCID01
    @Test
    void countUnread_hasUnread_shouldReturnCount() {
        UUID userId = UUID.randomUUID();

        when(repository.countByUserIdAndReadFalse(userId)).thenReturn(3L);

        UnreadCountResponse response = notificationService.countUnread(userId);

        assertThat(response).isEqualTo(new UnreadCountResponse(3L));
    }

    // UTCID02
    @Test
    void countUnread_noUnread_shouldReturnZero() {
        UUID userId = UUID.randomUUID();

        when(repository.countByUserIdAndReadFalse(userId)).thenReturn(0L);

        UnreadCountResponse response = notificationService.countUnread(userId);

        assertThat(response).isEqualTo(new UnreadCountResponse(0L));
    }

    // create
    // UTCID01
    @Test
    void create_allParams_shouldSaveNotificationWithGivenFields() {
        UUID userId = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        NotificationRefType refType = NotificationRefType.values()[0]; // thay bằng giá trị enum thật

        notificationService.create(userId, "Title", "Content", refType, refId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getTitle()).isEqualTo("Title");
        assertThat(saved.getContent()).isEqualTo("Content");
        assertThat(saved.getRefType()).isEqualTo(refType);
        assertThat(saved.getRefId()).isEqualTo(refId);
    }

    // UTCID02
    @Test
    void create_nullRef_shouldSaveWithoutReference() {
        UUID userId = UUID.randomUUID();

        notificationService.create(userId, "Title", "Content", null, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getRefType()).isNull();
        assertThat(captor.getValue().getRefId()).isNull();
    }

    // create for users
    // UTCID01
    @Test
    void createForUsers_multipleUsers_shouldSaveOneNotificationPerUser() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();
        UUID refId = UUID.randomUUID();
        NotificationRefType refType = NotificationRefType.values()[0]; // thay bằng giá trị enum thật

        notificationService.createForUsers(List.of(u1, u2, u3), "Title", "Content", refType, refId);

        verify(repository).saveAll(listCaptor.capture());
        List<Notification> saved = listCaptor.getValue();
        assertThat(saved).hasSize(3);
        assertThat(saved).extracting(Notification::getUserId).containsExactly(u1, u2, u3);
        assertThat(saved).allSatisfy(n -> {
            assertThat(n.getTitle()).isEqualTo("Title");
            assertThat(n.getContent()).isEqualTo("Content");
            assertThat(n.getRefType()).isEqualTo(refType);
            assertThat(n.getRefId()).isEqualTo(refId);
        });
    }

    // UTCID02
    @Test
    void createForUsers_emptyUsers_shouldSaveEmptyList() {
        notificationService.createForUsers(List.of(), "Title", "Content", null, null);

        verify(repository).saveAll(listCaptor.capture());
        assertThat(listCaptor.getValue()).isEmpty();
    }
}
