package com.manhnv.capstonebakerai.media.scheduler;

import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import com.manhnv.capstonebakerai.media.repository.MediaRepository;
import com.manhnv.capstonebakerai.media.service.MediaCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MediaCleanupScheduler {

    private static final Duration TTL = Duration.ofHours(24);
    private static final int BATCH = 100;

    private final MediaRepository mediaRepository;
    private final MediaCleanupService cleanupService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Ho_Chi_Minh")
    public void run() {
        Instant before = Instant.now().minus(TTL);
        int total = 0;
        while (true) {
            List<UUID> ids = mediaRepository.findCleanupIds(
                    MediaStatus.TEMP, MediaStatus.DELETING, before, PageRequest.of(0, BATCH));
            if (ids.isEmpty()) break;

            int deleted = 0;
            for (UUID id : ids) {
                try {
                    if (cleanupService.deleteIfTempOrDeleting(id)) deleted++;
                } catch (Exception e) {
                    log.warn("Không xóa được media {}; sẽ thử lại ở lần chạy sau", id, e);
                }
            }
            total += deleted;
            if (deleted == 0) break;
        }
        log.info("Media cleanup: đã xóa {} file", total);
    }
}
