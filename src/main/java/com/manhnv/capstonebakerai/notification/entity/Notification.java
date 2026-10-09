package com.manhnv.capstonebakerai.notification.entity;

import com.manhnv.capstonebakerai.notification.enums.NotificationRefType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", length = 255)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", length = 50)
    private NotificationRefType refType;

    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
