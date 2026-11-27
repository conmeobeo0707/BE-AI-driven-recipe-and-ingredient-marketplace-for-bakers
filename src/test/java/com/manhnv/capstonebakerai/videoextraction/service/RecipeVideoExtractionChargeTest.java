package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.loyalty.enums.AiFeature;
import com.manhnv.capstonebakerai.loyalty.service.LoyaltyService;
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
import com.manhnv.capstonebakerai.videoextraction.config.RecipeVideoExtractionProperties;
import com.manhnv.capstonebakerai.videoextraction.exception.RecipeVideoExtractionErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecipeVideoExtractionChargeTest {

    private final RecipeVideoExtractionProperties properties = new RecipeVideoExtractionProperties();
    private final MediaRepository mediaRepository = mock(MediaRepository.class);
    private final R2StorageService storageService = mock(R2StorageService.class);
    private final GeminiVideoClient geminiClient = mock(GeminiVideoClient.class);
    private final ExtractedRecipeMatcher matcher = mock(ExtractedRecipeMatcher.class);
    private final ExtractedRecipePersister persister = mock(ExtractedRecipePersister.class);
    private final RecipeService recipeService = mock(RecipeService.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final LoyaltyService loyaltyService = mock(LoyaltyService.class);

    private final RecipeVideoExtractionServiceImpl service = new RecipeVideoExtractionServiceImpl(
            properties, mediaRepository, storageService, geminiClient, new ExtractedRecipeSanitizer(),
            matcher, persister, recipeService, customerRepository, new PaidAiRunner(loyaltyService));

    private final UUID userId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID usageId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();
    private MockedStatic<SecurityUtils> security;

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::getCurrentUserId).thenReturn(userId);
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerRepository.findByUser_UserId(userId)).thenReturn(Optional.of(customer));
        when(loyaltyService.chargeForAi(customerId, AiFeature.RECIPE_VIDEO)).thenReturn(usageId);
        when(matcher.categoryNameHints()).thenReturn(List.of());
        givenVideo("videos/cake.mp4");
    }

    @AfterEach
    void tearDown() {
        security.close();
    }

    @Test
    void chargesRecipeVideoAndReturnsSavedRecipe() {
        UUID recipeId = UUID.randomUUID();
        RecipeResponse detail = RecipeResponse.builder().id(recipeId).build();
        when(geminiClient.extractRecipe(any(), eq("video/mp4"), anyString())).thenReturn(recipe(true));
        when(persister.save(eq(mediaId), eq(userId), any())).thenReturn(recipeId);
        when(recipeService.getDetail(recipeId)).thenReturn(detail);

        assertThat(service.extractAndSave(mediaId)).isSameAs(detail);

        verify(loyaltyService).chargeForAi(customerId, AiFeature.RECIPE_VIDEO);
        verify(loyaltyService, never()).refundAiError(any(), any());
    }

    @Test
    void refundsWhenAiFails() {
        when(geminiClient.extractRecipe(any(), any(), any()))
                .thenThrow(new AppException(RecipeVideoExtractionErrorCode.AI_UNAVAILABLE));

        assertThatThrownBy(() -> service.extractAndSave(mediaId)).isInstanceOf(AppException.class);

        verify(loyaltyService).refundAiError(eq(usageId), any());
        verifyNoInteractions(persister);
    }

    @Test
    void keepsPointsWhenVideoIsNotARecipe() {
        when(geminiClient.extractRecipe(any(), any(), any())).thenReturn(recipe(false));

        assertThatThrownBy(() -> service.extractAndSave(mediaId))
                .isInstanceOfSatisfying(AppException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(RecipeVideoExtractionErrorCode.NOT_A_RECIPE));

        verify(loyaltyService, never()).refundAiError(any(), any());
        verifyNoInteractions(persister);
    }

    @Test
    void doesNotChargeForAnInvalidVideo() {
        givenVideo("videos/cake.avi");

        assertThatThrownBy(() -> service.extractAndSave(mediaId)).isInstanceOf(AppException.class);

        verify(loyaltyService, never()).chargeForAi(any(), any());
        verifyNoInteractions(geminiClient);
    }

    @Test
    void refundsWhenSavingFails() {
        when(geminiClient.extractRecipe(any(), any(), any())).thenReturn(recipe(true));
        when(persister.save(any(), any(), any())).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.extractAndSave(mediaId)).isInstanceOf(IllegalStateException.class);

        verify(loyaltyService).refundAiError(eq(usageId), any());
    }

    private void givenVideo(String objectKey) {
        Media media = Media.builder().id(mediaId).type(MediaType.VIDEO).url("https://cdn/" + objectKey).build();
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(storageService.publicObjectKey(media.getUrl())).thenReturn(Optional.of(objectKey));
        when(storageService.contentLength(R2BucketType.PUBLIC, objectKey)).thenReturn(1024L);
        when(storageService.download(R2BucketType.PUBLIC, objectKey)).thenReturn(new byte[]{1, 2, 3});
    }

    private static RawExtractedRecipe recipe(boolean isRecipe) {
        return new RawExtractedRecipe(isRecipe, null, "Bánh", null, null, null, null, null,
                List.of(), List.of(), List.of());
    }
}
