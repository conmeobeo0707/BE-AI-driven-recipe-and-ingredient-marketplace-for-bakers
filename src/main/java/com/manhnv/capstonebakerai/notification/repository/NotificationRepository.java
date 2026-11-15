package com.manhnv.capstonebakerai.notification.repository;

import com.manhnv.capstonebakerai.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByUserId(UUID userId, Pageable pageable);
    Page<Notification> findByUserIdAndRead(UUID userId, boolean read, Pageable pageable);
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
    long countByUserIdAndReadFalse(UUID userId);
    @Modifying(clearAutomatically = true)
    @Query("""
        update Notification n
           set n.read = true, n.readAt = :now
         where n.userId = :userId and n.read = false
        """)
    int markAllAsRead(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
