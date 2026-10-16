
package com.manhnv.capstonebakerai.media.service.impl;

import com.manhnv.capstonebakerai.media.dto.response.MediaResponse;
import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.media.enums.MediaStatus;
import com.manhnv.capstonebakerai.media.enums.MediaType;
import com.manhnv.capstonebakerai.media.service.MediaCleanupService;
import com.manhnv.capstonebakerai.media.enums.R2BucketType;
import com.manhnv.capstonebakerai.media.repository.MediaRepository;
import com.manhnv.capstonebakerai.media.service.MediaService;
import com.manhnv.capstonebakerai.media.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {

    private static final int MAX_IMAGE_COUNT = 10;

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;

    private static final long MAX_IMAGE_TOTAL_SIZE = 25L * 1024 * 1024;

    private final R2StorageService storageService;
    private final MediaRepository mediaRepository;
    private final MediaCleanupService cleanupService;

    @Override
    @Transactional
    public List<MediaResponse> uploadImages(List<MultipartFile> files) {
        requireFiles(
                files,
                MAX_IMAGE_COUNT,
                MAX_IMAGE_SIZE,
                MAX_IMAGE_TOTAL_SIZE
        );
        List<PreparedFile> preparedFiles = prepareImages(files);
        return uploadPreparedFiles(
                preparedFiles,
                MediaType.IMAGE,
                "images"
        );
    }

    @Override
    @Transactional
    public MediaResponse uploadVideo(MultipartFile file) {
        List<MultipartFile> files = java.util.Collections.singletonList(file);
        requireFiles(
                files,
                1,
                MAX_VIDEO_SIZE,
                MAX_VIDEO_SIZE
        );
        List<PreparedFile> preparedFiles = prepareVideos(files);
        return uploadPreparedFiles(
                preparedFiles,
                MediaType.VIDEO,
                "videos"
        ).get(0);
    }

    @Override
    public MediaResponse deleteMedia(UUID id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Media not found: " + id));
        if (media.getStatus() == MediaStatus.ATTACHED) {
            throw new IllegalArgumentException("Cannot delete a media attached to a recipe");
        }

        media.setStatus(MediaStatus.DELETING);
        mediaRepository.saveAndFlush(media);

        try {
            cleanupService.deleteIfTempOrDeleting(id);
        } catch (Exception e) {
            log.warn("Chưa xóa được media {}; scheduler 3h sáng sẽ retry", id, e);
        }

        return MediaResponse.builder()
                .id(media.getId())
                .url(media.getUrl())
                .type(media.getType())
                .status(MediaStatus.DELETING)
                .build();
    }

    private List<PreparedFile> prepareImages(List<MultipartFile> files) {
        List<PreparedFile> preparedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                byte[] content = file.getBytes();
                validateActualSize(content, MAX_IMAGE_SIZE);

                DetectedFile detected = detectImage(content);
                preparedFiles.add(new PreparedFile(content, detected));
            } catch (IOException exception) {
                throw new IllegalArgumentException(
                        "Could not read image file: "
                                + file.getOriginalFilename(),
                        exception
                );
            }
        }
        return preparedFiles;
    }

    private List<PreparedFile> prepareVideos(List<MultipartFile> files) {
        List<PreparedFile> preparedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                byte[] content = file.getBytes();
                validateActualSize(content, MAX_VIDEO_SIZE);
                DetectedFile detected = detectVideo(
                        content,
                        file.getContentType(),
                        file.getOriginalFilename()
                );
                preparedFiles.add(new PreparedFile(content, detected));
            } catch (IOException exception) {
                throw new IllegalArgumentException(
                        "Could not read video file: "
                                + file.getOriginalFilename(),
                        exception
                );
            }
        }
        return preparedFiles;
    }

    private List<MediaResponse> uploadPreparedFiles(List<PreparedFile> files, MediaType mediaType, String directory) {
        List<String> uploadedKeys = new ArrayList<>();
        boolean synchronizationActive =
                TransactionSynchronizationManager.isSynchronizationActive();
        if (synchronizationActive) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                cleanupUploadedFiles(uploadedKeys);
                            }
                        }
                    }
            );
        }
        try {
            List<MediaResponse> responses = new ArrayList<>();
            for (PreparedFile file : files) {
                responses.add(upload(
                        file.content(),
                        file.detected(),
                        mediaType,
                        directory,
                        uploadedKeys
                ));
            }
            return responses;
        } catch (RuntimeException exception) {
            if (!synchronizationActive) {
                cleanupUploadedFiles(uploadedKeys);
            }
            throw exception;
        }
    }

    private MediaResponse upload(byte[] content, DetectedFile detected, MediaType mediaType, String directory, List<String> uploadedKeys) {
        String key = "media/" + directory + "/"
                + UUID.randomUUID() + "."
                + detected.extension();
        uploadedKeys.add(key);
        storageService.upload(
                R2BucketType.PUBLIC,
                key,
                content,
                detected.contentType()
        );
        String url = storageService.publicUrl(key);
        Media media = mediaRepository.save(
                Media.builder()
                        .status(MediaStatus.TEMP)
                        .tempSince(Instant.now())
                        .type(mediaType)
                        .url(url)
                        .build()
        );
        return MediaResponse.builder()
                .id(media.getId())
                .url(media.getUrl())
                .type(media.getType())
                .status(media.getStatus())
                .build();
    }

    private void cleanupUploadedFiles(List<String> objectKeys) {
        for (String key : objectKeys) {
            try {
                storageService.delete(R2BucketType.PUBLIC, key);
            } catch (RuntimeException exception) {
                log.error(
                        "Failed to clean up uploaded media from R2: {}",
                        key,
                        exception
                );
            }
        }
    }

    private void requireFiles(List<MultipartFile> files, int maxCount, long maxFileSize, long maxTotalSize) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "Files are required and cannot be empty"
            );
        }
        if (files.size() > maxCount) {
            throw new IllegalArgumentException(
                    "Too many files. Maximum allowed: " + maxCount
            );
        }
        long totalSize = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(
                        "One or more files are required and cannot be empty"
                );
            }

            long size = file.getSize();
            if (size > maxFileSize) {
                throw new IllegalArgumentException(
                        "File exceeds the maximum allowed size of "
                                + maxFileSize + " bytes: "
                                + file.getOriginalFilename()
                );
            }
            if (size > maxTotalSize - totalSize) {
                throw new IllegalArgumentException(
                        "Total file size exceeds the maximum allowed size of "
                                + maxTotalSize + " bytes"
                );
            }
            totalSize += size;
        }
    }

    private void validateActualSize(byte[] content, long maxFileSize) {
        if (content.length == 0 || content.length > maxFileSize) {
            throw new IllegalArgumentException(
                    "Actual file content is empty or exceeds the size limit"
            );
        }
    }

    private DetectedFile detectImage(byte[] bytes) {
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
            return new DetectedFile("jpg", "image/jpeg");
        }

        if (startsWith(
                bytes,
                0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        )) {
            return new DetectedFile("png", "image/png");
        }

        if (bytes.length >= 12
                && ascii(bytes, 0, 4).equals("RIFF")
                && ascii(bytes, 8, 4).equals("WEBP")) {
            return new DetectedFile("webp", "image/webp");
        }

        throw new IllegalArgumentException(
                "Only valid JPEG, PNG, and WebP images are supported"
        );
    }

    private DetectedFile detectVideo(byte[] bytes, String contentType, String filename) {
        String mime = contentType == null
                ? ""
                : contentType.toLowerCase(Locale.ROOT);
        String extension = extension(filename);
        if (startsWith(bytes, 0x1A, 0x45, 0xDF, 0xA3)) {
            return new DetectedFile("webm", "video/webm");
        }
        if (bytes.length >= 12
                && ascii(bytes, 4, 4).equals("ftyp")) {
            boolean quickTime =
                    mime.equals("video/quicktime")
                            || extension.equals("mov");
            if (!mime.isEmpty()
                    && !mime.equals("application/octet-stream")
                    && !mime.equals("video/mp4")
                    && !mime.equals("video/quicktime")
                    && !mime.equals("video/x-m4v")) {
                throw new IllegalArgumentException(
                        "Unsupported video format"
                );
            }
            return quickTime
                    ? new DetectedFile("mov", "video/quicktime")
                    : new DetectedFile("mp4", "video/mp4");
        }
        throw new IllegalArgumentException(
                "Only valid MP4, MOV, and WebM videos are supported"
        );
    }

    private boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String ascii(byte[] bytes, int offset, int length) {
        return new String(
                bytes,
                offset,
                length,
                StandardCharsets.US_ASCII
        );
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0
                ? ""
                : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private record DetectedFile(String extension, String contentType) {
    }

    private record PreparedFile(byte[] content, DetectedFile detected) {
    }
}
