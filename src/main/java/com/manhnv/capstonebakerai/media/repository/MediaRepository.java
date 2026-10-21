package com.manhnv.capstonebakerai.media.repository;

import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    @Query("""
        SELECT m.id FROM Media m
        WHERE m.status = :deleting
           OR (m.status = :temp AND m.tempSince < :before)
        """)
    List<UUID> findCleanupIds(@Param("temp") MediaStatus temp,
                              @Param("deleting") MediaStatus deleting,
                              @Param("before") Instant before,
                              Pageable pageable);

    @Query("SELECT m.id FROM Media m WHERE m.status = :status")
    List<UUID> findIdsByStatus(@Param("status") MediaStatus status, Pageable pageable);
}
