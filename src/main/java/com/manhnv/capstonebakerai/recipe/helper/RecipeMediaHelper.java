package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import com.manhnv.capstonebakerai.media.enums.MediaType;
import com.manhnv.capstonebakerai.media.repository.MediaRepository;
import com.manhnv.capstonebakerai.media.service.MediaCleanupService;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepImage;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeMediaHelper {

    private final MediaRepository mediaRepository;
    private final RecipeStepImageRepository recipeStepImageRepository;
    private final MediaCleanupService mediaCleanupService;

    public void validateContinuousStepNumbers(Collection<Integer> stepNumbers) {
        if (stepNumbers == null || stepNumbers.isEmpty()) return;

        Set<Integer> unique = new HashSet<>(stepNumbers);
        if (unique.size() != stepNumbers.size()) {
            throw new RuntimeException("Step number already exists in recipe");
        }

        int max = Collections.max(unique);
        for (int i = 1; i <= max; i++) {
            if (!unique.contains(i)) {
                throw new RuntimeException("Step number must be continuous starting from 1");
            }
        }
    }

    public void validateUniqueMediaIds(List<UUID> mediaIds) {
        if (mediaIds == null) return;
        if (mediaIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Media IDs cannot contain null values");
        }
        if (new HashSet<>(mediaIds).size() != mediaIds.size()) {
            throw new IllegalArgumentException("Media IDs cannot contain duplicates");
        }
    }

    public Map<UUID, Media> loadMediaMap(Collection<UUID> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) return new HashMap<>();
        if (requestedIds.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Media IDs cannot contain null values");
        }

        Set<UUID> uniqueIds = new HashSet<>(requestedIds);
        Map<UUID, Media> mediaById = new HashMap<>();
        for (Media media : mediaRepository.findAllById(uniqueIds)) {
            mediaById.put(media.getId(), media);
        }

        if (mediaById.size() != uniqueIds.size()) {
            Set<UUID> missing = new HashSet<>(uniqueIds);
            missing.removeAll(mediaById.keySet());
            throw new IllegalArgumentException("Media not found: " + missing);
        }
        return mediaById;
    }

    public Media resolveMedia(UUID mediaId, MediaType expectedType, Map<UUID, Media> mediaById,
                              Set<UUID> currentlyAttachedMediaIds) {
        Media media = mediaById.get(mediaId);
        if (media == null) throw new IllegalArgumentException("Media not found: " + mediaId);
        if (media.getType() != expectedType) {
            throw new IllegalArgumentException("Media " + mediaId + " must have type " + expectedType);
        }
        if (media.getStatus() != MediaStatus.TEMP && !currentlyAttachedMediaIds.contains(mediaId)) {
            throw new IllegalArgumentException("Media " + mediaId + " is already attached to another resource");
        }
        media.setStatus(MediaStatus.ATTACHED);
        media.setTempSince(null);
        return media;
    }

    public void saveStepImages(RecipeStep step, List<UUID> mediaIds, Map<UUID, Media> mediaById,
                               Set<UUID> currentlyAttachedMediaIds) {
        validateUniqueMediaIds(mediaIds);
        List<RecipeStepImage> links = new ArrayList<>();
        for (int index = 0; index < mediaIds.size(); index++) {
            links.add(RecipeStepImage.builder()
                    .recipeStep(step)
                    .media(resolveMedia(mediaIds.get(index), MediaType.IMAGE, mediaById, currentlyAttachedMediaIds))
                    .sortOrder(index + 1)
                    .build());
        }
        recipeStepImageRepository.saveAll(links);
    }

    public void detachMedia(Collection<Media> medias) {
        Instant now = Instant.now();
        for (Media media : medias) {
            media.setStatus(MediaStatus.DELETING);
            media.setTempSince(now);
        }
    }

    public void deleteMediaAfterCommit(List<UUID> mediaIds) {
        if (mediaIds.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (UUID id : mediaIds) {
                    try {
                        mediaCleanupService.deleteIfTempOrDeleting(id);
                    } catch (Exception e) {
                        log.warn("Chưa xóa được media {}, job ban đêm sẽ retry", id, e);
                    }
                }
            }
        });
    }
}
