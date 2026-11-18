package com.manhnv.capstonebakerai.recipe.suggestion.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.loyalty.enums.AiFeature;
import com.manhnv.capstonebakerai.loyalty.service.LoyaltyService;
import com.manhnv.capstonebakerai.loyalty.service.PaidAiRunner;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientCatalogCandidate;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientNormalizer;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientResolver;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientCatalogLookupService;
import com.manhnv.capstonebakerai.recipe.suggestion.client.GroqIngredientVisionClient;
import com.manhnv.capstonebakerai.recipe.suggestion.client.GroqIngredientVisionClient.DetectedIngredient;
import com.manhnv.capstonebakerai.recipe.suggestion.config.IngredientVisionProperties;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientImageRecognitionResponse;
import com.manhnv.capstonebakerai.recipe.suggestion.exception.IngredientImageErrorCode;
import com.manhnv.capstonebakerai.recipe.suggestion.helper.IngredientImagePreprocessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class IngredientImageServiceImpl implements IngredientImageService {

    private static final long SLOT_WAIT_SECONDS = 10;

    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CustomerRepository customerRepository;

    private final IngredientVisionProperties properties;

    private final IngredientImagePreprocessor imagePreprocessor;

    private final GroqIngredientVisionClient visionClient;

    private final IngredientNormalizer ingredientNormalizer;

    private final IngredientResolver ingredientResolver;
    private final IngredientCatalogLookupService lookupService;

    private final LoyaltyService loyaltyService;

    private final PaidAiRunner paidAiRunner;

    private final Semaphore recognitionSlots = new Semaphore(2);

    // Key is "<userId>:<date>", so the count starts again at midnight; entries outlive their day by a margin.
    private final Cache<String, AtomicInteger> dailyUsage = Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofHours(25))
            .build();

    @Override
    public IngredientImageRecognitionResponse recognize(
            MultipartFile file
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();

        UUID customerId = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(
                        CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND
                ))
                .getId();

        if (!properties.isEnabled()
                || properties.getApiKey() == null
                || properties.getApiKey().isBlank()
                || properties.getModel() == null
                || properties.getModel().isBlank()) {
            throw new AppException(
                    IngredientImageErrorCode.VISION_NOT_CONFIGURED
            );
        }

        // Validate the image first so an invalid file does not use up the customer's quota.
        String dataUrl = imagePreprocessor.toDataUrl(file);

        AtomicInteger usage = consumeDailyQuota(userId);

        List<DetectedIngredient> detectedIngredients;
        try {
            detectedIngredients = paidAiRunner.run(
                    customerId,
                    AiFeature.INGREDIENT_IMAGE,
                    () -> recognizeWithSlot(dataUrl),
                    IngredientImageServiceImpl::isRefused
            );
        } catch (AppException exception) {
            // The customer got no result (busy, network, timeout, Groq-side error or an unusable answer):
            // give the attempt back. A refused image keeps the attempt so it cannot be retried for free.
            if (exception.getErrorCode() != IngredientImageErrorCode.VISION_REFUSED) {
                usage.decrementAndGet();
            }
            throw exception;
        } catch (RuntimeException exception) {
            usage.decrementAndGet();
            throw exception;
        }

        return toResponse(
                detectedIngredients,
                Math.max(0, properties.getDailyLimit() - usage.get()),
                loyaltyService.priceOf(AiFeature.INGREDIENT_IMAGE),
                loyaltyService.getBalance(customerId)
        );
    }

    private List<DetectedIngredient> recognizeWithSlot(String dataUrl) {
        acquireSlot();
        try {
            return visionClient.recognize(dataUrl);
        } finally {
            recognitionSlots.release();
        }
    }

    private static boolean isRefused(RuntimeException exception) {
        return exception instanceof AppException appException
                && appException.getErrorCode() == IngredientImageErrorCode.VISION_REFUSED;
    }

    private AtomicInteger consumeDailyQuota(UUID userId) {
        String key = userId + ":" + LocalDate.now(QUOTA_ZONE);
        AtomicInteger usage = dailyUsage.get(key, ignored -> new AtomicInteger());

        if (usage.incrementAndGet() > properties.getDailyLimit()) {
            usage.decrementAndGet();
            throw new AppException(
                    IngredientImageErrorCode.IMAGE_RATE_LIMIT_EXCEEDED,
                    "You have used all " + properties.getDailyLimit()
                            + " image recognitions for today. Please try again tomorrow or enter ingredients manually"
            );
        }
        return usage;
    }

    private void acquireSlot() {
        try {
            if (!recognitionSlots.tryAcquire(SLOT_WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new AppException(
                        IngredientImageErrorCode.VISION_UNAVAILABLE
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException(
                    IngredientImageErrorCode.VISION_UNAVAILABLE
            );
        }
    }

    private IngredientImageRecognitionResponse toResponse(
            List<DetectedIngredient> detectedIngredients,
            int remainingDailyQuota,
            int pointsCharged,
            int pointsBalance
    ) {
        Map<String, DetectedIngredient> uniqueNames = new LinkedHashMap<>();

        for (DetectedIngredient item : detectedIngredients) {
            String normalized = ingredientNormalizer.normalize(item.name());

            if (normalized.isBlank()) {
                continue;
            }

            uniqueNames.merge(normalized, item, this::mergeDetected);
        }

        // Matched items are keyed by catalog ID so two names for the same ingredient become one row.
        Map<String, IngredientImageRecognitionResponse.Item> items = new LinkedHashMap<>();
        List<DetectedIngredient> unmatched = new ArrayList<>();

        for (DetectedIngredient item : uniqueNames.values()) {
            Optional<IngredientCatalog> resolved = ingredientResolver.resolveExact(item.name())
                    .or(() -> ingredientResolver.resolveExact(item.nameVi()));

            if (resolved.isEmpty()) {
                unmatched.add(item);
                continue;
            }

            IngredientCatalog catalog = resolved.get();
            items.merge(
                    catalog.getId().toString(),
                    new IngredientImageRecognitionResponse.Item(
                            item.name(),
                            item.nameVi(),
                            item.uncertain(),
                            true,
                            catalog.getId(),
                            catalog.getCanonicalName(),
                            List.of()
                    ),
                    (first, second) -> new IngredientImageRecognitionResponse.Item(
                            first.observedName(),
                            first.vietnameseName(),
                            first.uncertain() || second.uncertain(),
                            true,
                            first.catalogIngredientId(),
                            first.canonicalName(),
                            List.of()
                    )
            );
        }

        List<String> lookupNames = new ArrayList<>();
        for (DetectedIngredient item : unmatched) {
            lookupNames.add(item.name());
            if (!item.nameVi().isBlank()) {
                lookupNames.add(item.nameVi());
            }
        }
        Map<String, List<IngredientCatalogCandidate>> candidates = lookupService.searchAll(lookupNames, 3);

        for (DetectedIngredient item : unmatched) {
            List<IngredientCatalogCandidate> found = candidates.getOrDefault(item.name(), List.of());
            if (found.isEmpty()) {
                found = candidates.getOrDefault(item.nameVi(), List.of());
            }
            items.put(
                    "unmatched:" + ingredientNormalizer.normalize(item.name()),
                    new IngredientImageRecognitionResponse.Item(
                            item.name(),
                            item.nameVi(),
                            item.uncertain(),
                            false,
                            null,
                            null,
                            found
                    )
            );
        }

        return new IngredientImageRecognitionResponse(
                remainingDailyQuota,
                pointsCharged,
                pointsBalance,
                List.copyOf(items.values())
        );
    }

    private DetectedIngredient mergeDetected(
            DetectedIngredient first,
            DetectedIngredient second
    ) {
        return new DetectedIngredient(
                first.name(),
                first.nameVi().isBlank() ? second.nameVi() : first.nameVi(),
                first.uncertain() || second.uncertain()
        );
    }
}
