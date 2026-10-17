package com.manhnv.capstonebakerai.media.service.impl;

import com.manhnv.capstonebakerai.config.R2Properties;
import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import com.manhnv.capstonebakerai.media.enums.R2BucketType;
import com.manhnv.capstonebakerai.media.repository.MediaRepository;
import com.manhnv.capstonebakerai.media.service.MediaCleanupService;
import com.manhnv.capstonebakerai.media.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaCleanupServiceImpl implements MediaCleanupService {

    private final MediaRepository mediaRepository;
    private final R2StorageService r2StorageService;
    private final R2Properties properties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean deleteIfTempOrDeleting(UUID id) {
        Media media = mediaRepository.findById(id).orElse(null);
        if (media == null
                || (media.getStatus() != MediaStatus.TEMP && media.getStatus() != MediaStatus.DELETING)) {
            return false;
        }
        r2StorageService.delete(R2BucketType.PUBLIC, extractObjectKey(media.getUrl()));
        mediaRepository.delete(media);
        return true;
    }

    private String extractObjectKey(String url) {
        String base = properties.publicUrl().replaceAll("/+$", "");
        if (url == null || !url.startsWith(base + "/")) {
            throw new IllegalStateException("URL không thuộc R2 public URL: " + url);
        }
        return url.substring(base.length() + 1);
    }
}
