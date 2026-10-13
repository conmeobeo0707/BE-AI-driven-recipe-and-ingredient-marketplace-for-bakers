package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.media.enums.R2BucketType;
import com.manhnv.capstonebakerai.media.service.R2StorageService;
import com.manhnv.capstonebakerai.recipe.dto.response.MediaResponse;
import com.manhnv.capstonebakerai.recipe.entity.Media;
import com.manhnv.capstonebakerai.recipe.enums.MediaStatus;
import com.manhnv.capstonebakerai.recipe.enums.MediaType;
import com.manhnv.capstonebakerai.recipe.repository.MediaRepository;
import com.manhnv.capstonebakerai.recipe.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final R2StorageService storageService;
    private final MediaRepository mediaRepository;

    @Override
    @Transactional
    public MediaResponse uploadImage(MultipartFile file) {
        requireFile(file);
        try {
            byte[] content = file.getBytes();
            DetectedFile detected = detectImage(content);
            return upload(content, detected, MediaType.IMAGE, "images");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read image file", exception);
        }
    }

    @Override
    @Transactional
    public MediaResponse uploadVideo(MultipartFile file) {
        requireFile(file);
        try {
            byte[] content = file.getBytes();
            DetectedFile detected = detectVideo(content, file.getContentType(), file.getOriginalFilename());
            return upload(content, detected, MediaType.VIDEO, "videos");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read video file", exception);
        }
    }

    private MediaResponse upload(byte[] content, DetectedFile detected,
                                 MediaType mediaType, String directory) {
        String key = "media/" + directory + "/" + UUID.randomUUID() + "." + detected.extension();
        storageService.upload(R2BucketType.PUBLIC, key, content, detected.contentType());
        String url = storageService.publicUrl(key);

        Media media = mediaRepository.save(Media.builder()
                .status(MediaStatus.TEMP)
                .type(mediaType)
                .url(url)
                .build());

        return MediaResponse.builder()
                .id(media.getId())
                .url(media.getUrl())
                .type(media.getType())
                .status(media.getStatus())
                .build();
    }

    private void requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and cannot be empty");
        }
    }

    private DetectedFile detectImage(byte[] bytes) {
        if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) return new DetectedFile("jpg", "image/jpeg");
        if (startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return new DetectedFile("png", "image/png");
        }
        if (bytes.length >= 12
                && ascii(bytes, 0, 4).equals("RIFF")
                && ascii(bytes, 8, 4).equals("WEBP")) {
            return new DetectedFile("webp", "image/webp");
        }
        throw new IllegalArgumentException("Only valid JPEG, PNG, and WebP images are supported");
    }

    private DetectedFile detectVideo(byte[] bytes, String contentType, String filename) {
        String mime = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String extension = extension(filename);

        if (startsWith(bytes, 0x1A, 0x45, 0xDF, 0xA3)) {
            return new DetectedFile("webm", "video/webm");
        }
        if (bytes.length >= 12 && ascii(bytes, 4, 4).equals("ftyp")) {
            boolean quickTime = mime.equals("video/quicktime") || extension.equals("mov");
            if (!mime.isEmpty() && !mime.equals("application/octet-stream")
                    && !mime.equals("video/mp4") && !mime.equals("video/quicktime")
                    && !mime.equals("video/x-m4v")) {
                throw new IllegalArgumentException("Unsupported video format");
            }
            return quickTime
                    ? new DetectedFile("mov", "video/quicktime")
                    : new DetectedFile("mp4", "video/mp4");
        }
        throw new IllegalArgumentException("Only valid MP4, MOV, and WebM videos are supported");
    }

    private boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    private String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private record DetectedFile(String extension, String contentType) {
    }
}
