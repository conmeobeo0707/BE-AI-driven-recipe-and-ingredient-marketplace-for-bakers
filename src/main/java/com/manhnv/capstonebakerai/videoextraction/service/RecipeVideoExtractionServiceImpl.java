package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.loyalty.enums.AiFeature;
import com.manhnv.capstonebakerai.loyalty.service.PaidAiRunner;
import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.media.enums.MediaType;
import com.manhnv.capstonebakerai.media.enums.R2BucketType;
import com.manhnv.capstonebakerai.media.repository.MediaRepository;
import com.manhnv.capstonebakerai.media.service.R2StorageService;
import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;
import com.manhnv.capstonebakerai.recipe.service.RecipeService;
import com.manhnv.capstonebakerai.videoextraction.client.GeminiVideoClient;
import com.manhnv.capstonebakerai.videoextraction.client.RawExtractedRecipe;
import com.manhnv.capstonebakerai.videoextraction.client.RecipeExtractionPrompt;
import com.manhnv.capstonebakerai.videoextraction.config.RecipeVideoExtractionProperties;
import com.manhnv.capstonebakerai.videoextraction.dto.RecipeVideoExtractionResponse;
import com.manhnv.capstonebakerai.videoextraction.exception.RecipeVideoExtractionErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeVideoExtractionServiceImpl implements RecipeVideoExtractionService {

    private final RecipeVideoExtractionProperties properties;
    private final MediaRepository mediaRepository;
    private final R2StorageService storageService;
    private final GeminiVideoClient geminiClient;
    private final ExtractedRecipeSanitizer sanitizer;
    private final ExtractedRecipeMatcher matcher;
    private final ExtractedRecipePersister persister;
    private final RecipeService recipeService;
    private final CustomerRepository customerRepository;
    private final PaidAiRunner paidAiRunner;

    @Override
    public RecipeResponse extractAndSave(UUID mediaId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID customerId = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND))
                .getId();
        VideoSource source = prepare(mediaId);

        UUID recipeId = paidAiRunner.run(
                customerId,
                AiFeature.RECIPE_VIDEO,
                () -> {
                    ExtractedRecipe extracted = extract(source);
                    if (!extracted.isRecipe()) {
                        throw new AppException(RecipeVideoExtractionErrorCode.NOT_A_RECIPE);
                    }
                    return persister.save(mediaId, userId, extracted);
                },
                RecipeVideoExtractionServiceImpl::keepsPoints);
        return recipeService.getDetail(recipeId);
    }

    @Override
    public RecipeVideoExtractionResponse preview(UUID mediaId) {
        return matcher.match(mediaId, extract(prepare(mediaId)), false);
    }

    private VideoSource prepare(UUID mediaId) {
        if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AppException(RecipeVideoExtractionErrorCode.EXTRACTION_NOT_CONFIGURED);
        }

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new AppException(RecipeVideoExtractionErrorCode.MEDIA_NOT_FOUND));
        if (media.getType() != MediaType.VIDEO) {
            throw new AppException(RecipeVideoExtractionErrorCode.MEDIA_NOT_VIDEO);
        }

        String objectKey = storageService.publicObjectKey(media.getUrl())
                .orElseThrow(() -> new AppException(RecipeVideoExtractionErrorCode.VIDEO_NOT_READABLE));
        String mimeType = mimeTypeOf(objectKey);
        try {
            if (storageService.contentLength(R2BucketType.PUBLIC, objectKey) > properties.getMaxVideoBytes()) {
                throw new AppException(RecipeVideoExtractionErrorCode.VIDEO_TOO_LARGE);
            }
        } catch (SdkException exception) {
            log.warn("Could not read video {} from R2: {}", objectKey, exception.getMessage());
            throw new AppException(RecipeVideoExtractionErrorCode.VIDEO_NOT_READABLE);
        }
        return new VideoSource(mediaId, objectKey, mimeType);
    }

    private ExtractedRecipe extract(VideoSource source) {
        byte[] video;
        try {
            video = storageService.download(R2BucketType.PUBLIC, source.objectKey());
        } catch (SdkException exception) {
            log.warn("Could not read video {} from R2: {}", source.objectKey(), exception.getMessage());
            throw new AppException(RecipeVideoExtractionErrorCode.VIDEO_NOT_READABLE);
        }

        String prompt = RecipeExtractionPrompt.build(matcher.categoryNameHints());
        RawExtractedRecipe raw = geminiClient.extractRecipe(video, source.mimeType(), prompt);
        if (raw == null || raw.isRecipe() == null) {
            log.warn("Gemini recipe extraction returned no isRecipe flag for media {}", source.mediaId());
            throw new AppException(RecipeVideoExtractionErrorCode.AI_INVALID_RESPONSE);
        }
        return sanitizer.sanitize(raw);
    }

    private static boolean keepsPoints(RuntimeException exception) {
        return exception instanceof AppException appException
                && (appException.getErrorCode() == RecipeVideoExtractionErrorCode.AI_REFUSED
                || appException.getErrorCode() == RecipeVideoExtractionErrorCode.NOT_A_RECIPE);
    }

    private static String mimeTypeOf(String objectKey) {
        int dot = objectKey.lastIndexOf('.');
        String extension = dot < 0 ? "" : objectKey.substring(dot + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "mp4", "m4v" -> "video/mp4";
            case "mov" -> "video/mov";
            case "webm" -> "video/webm";
            default -> throw new AppException(RecipeVideoExtractionErrorCode.UNSUPPORTED_VIDEO_TYPE);
        };
    }

    private record VideoSource(UUID mediaId, String objectKey, String mimeType) {
    }
}
