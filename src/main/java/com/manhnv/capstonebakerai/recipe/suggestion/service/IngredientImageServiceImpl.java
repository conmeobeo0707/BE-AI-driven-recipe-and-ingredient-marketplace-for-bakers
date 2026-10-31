package com.manhnv.capstonebakerai.recipe.suggestion.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientNormalizer;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientResolver;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientCatalogLookupService;
import com.manhnv.capstonebakerai.recipe.suggestion.client.GroqIngredientVisionClient;
import com.manhnv.capstonebakerai.recipe.suggestion.client.GroqIngredientVisionClient.DetectedIngredient;
import com.manhnv.capstonebakerai.recipe.suggestion.config.IngredientVisionProperties;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientImageRecognitionResponse;
import com.manhnv.capstonebakerai.recipe.suggestion.exception.IngredientImageErrorCode;
import com.manhnv.capstonebakerai.recipe.suggestion.helper.IngredientImagePreprocessor;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
public class IngredientImageServiceImpl implements IngredientImageService {

    private final CustomerRepository customerRepository;

    private final IngredientVisionProperties properties;

    private final IngredientImagePreprocessor imagePreprocessor;

    private final GroqIngredientVisionClient visionClient;

    private final IngredientNormalizer ingredientNormalizer;

    private final IngredientResolver ingredientResolver;
    private final IngredientCatalogLookupService lookupService;

    private final Semaphore recognitionSlots = new Semaphore(2);

    private final Cache<UUID, Bucket> userBuckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    @Override
    public IngredientImageRecognitionResponse recognize(
            MultipartFile file
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();

        customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(
                        CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND
                ));

        if (!properties.isEnabled()
                || properties.getApiKey() == null
                || properties.getApiKey().isBlank()
                || properties.getModel() == null
                || properties.getModel().isBlank()) {
            throw new AppException(
                    IngredientImageErrorCode.VISION_NOT_CONFIGURED
            );
        }

        Bucket bucket = userBuckets.get(
                userId,
                ignored -> Bucket.builder()
                        .addLimit(limit -> limit
                                .capacity(5)
                                .refillGreedy(
                                        5,
                                        Duration.ofMinutes(1)
                                )
                        )
                        .build()
        );

        if (!bucket.tryConsume(1)) {
            throw new AppException(
                    IngredientImageErrorCode.IMAGE_RATE_LIMIT_EXCEEDED
            );
        }

        if (!recognitionSlots.tryAcquire()) {
            throw new AppException(
                    IngredientImageErrorCode.VISION_UNAVAILABLE
            );
        }

        try {
            String dataUrl = imagePreprocessor.toDataUrl(file);

            var detectedIngredients = visionClient.recognize(dataUrl);

            Map<String, DetectedIngredient> uniqueIngredients =
                    new LinkedHashMap<>();

            for (DetectedIngredient item : detectedIngredients) {
                String normalized =
                        ingredientNormalizer.normalize(item.name());

                if (normalized.isBlank()) {
                    continue;
                }

                uniqueIngredients.merge(
                        normalized,
                        item,
                        (first, second) -> new DetectedIngredient(
                                first.name(),
                                first.uncertain() || second.uncertain()
                        )
                );
            }

            var items = uniqueIngredients.values().stream()
                    .map(item -> {
                        var resolved =
                                ingredientResolver.resolveExact(item.name());

                        return new IngredientImageRecognitionResponse.Item(
                                item.name(),
                                item.uncertain(),
                                resolved.isPresent(),
                                resolved.map(catalog -> catalog.getId())
                                        .orElse(null),
                                resolved.map(
                                        catalog -> catalog.getCanonicalName()
                                ).orElse(null),
                                java.util.List.of()
                        );
                    })
                    .toList();

            var unmatchedNames = items.stream().filter(item -> !item.matched())
                    .map(IngredientImageRecognitionResponse.Item::observedName).toList();
            var candidates = lookupService.searchAll(unmatchedNames, 3);
            var withCandidates = items.stream().map(item -> new IngredientImageRecognitionResponse.Item(
                    item.observedName(), item.uncertain(), item.matched(), item.catalogIngredientId(),
                    item.canonicalName(), candidates.getOrDefault(item.observedName(), java.util.List.of())
            )).toList();

            return new IngredientImageRecognitionResponse(
                    "IMAGE",
                    true,
                    withCandidates
            );

        } finally {
            recognitionSlots.release();
        }
    }
}
