package com.manhnv.capstonebakerai.recipe.service;

import com.manhnv.capstonebakerai.media.dto.request.ShotstackWebhookRequest;
import com.manhnv.capstonebakerai.media.entity.Media;
import com.manhnv.capstonebakerai.media.enums.R2BucketType;
import com.manhnv.capstonebakerai.media.repository.MediaRepository;
import com.manhnv.capstonebakerai.media.service.R2StorageService;
import com.manhnv.capstonebakerai.media.service.impl.ShotstackServiceImpl;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeVideoRender;
import com.manhnv.capstonebakerai.recipe.helper.RecipeMediaHelper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeVideoRenderRepository;
import com.manhnv.capstonebakerai.recipe.service.impl.RecipeVideoServiceImpl;
import com.manhnv.capstonebakerai.recipe.video.VideoSlides;
import com.manhnv.capstonebakerai.recipe.video.VideoThemeSelector;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RecipeVideoWebhookFlowTest {
    private static final String VERIFIED_URL = "https://verified.test/video.mp4";
    private static final String STORED_URL = "https://cdn.test/media/videos/video.mp4";

    @Test
    void verifiedCompletionIsStoredAndAttachedBeforeWebhookReturns() {
        runWebhook("done", null, true);
    }

    @Test
    void providerFailureOverridesForgedSuccessInCallback() {
        runWebhook("failed", "Render failed", false);
    }

    private void runWebhook(String status, String error, boolean completed) {
        var builder = RestClient.builder().baseUrl("https://shotstack.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var renders = mock(RecipeVideoRenderRepository.class);
        var recipes = mock(RecipeRepository.class);
        var mediaRepository = mock(MediaRepository.class);
        var storage = mock(R2StorageService.class);
        var mediaHelper = mock(RecipeMediaHelper.class);
        var transactionManager = mock(PlatformTransactionManager.class);
        var recipeId = UUID.randomUUID();
        var render = RecipeVideoRender.builder().recipeId(recipeId)
                .renderId("render-1").status("PROCESSING").build();
        when(renders.findByRenderId("render-1")).thenReturn(Optional.of(render));
        when(renders.findById(any())).thenReturn(Optional.of(render));
        var recipe = new Recipe();
        byte[] videoBytes = {1, 2, 3};
        if (completed) {
            when(recipes.findById(recipeId)).thenReturn(Optional.of(recipe));
            when(mediaRepository.save(any(Media.class))).thenAnswer(call -> call.getArgument(0));
            when(storage.publicUrl(anyString())).thenReturn(STORED_URL);
        }
        String body = "{\"response\":{\"status\":\"" + status + "\""
                + (completed ? ",\"url\":\"" + VERIFIED_URL + "\"" : ",\"error\":\"" + error + "\"") + "}}";
        server.expect(requestTo("https://shotstack.test/render/render-1"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        try (var context = new AnnotationConfigApplicationContext()) {
            var shotstack = spy(new ShotstackServiceImpl(builder.build(), context));
            doReturn(videoBytes).when(shotstack).downloadOutput(VERIFIED_URL);
            var service = new RecipeVideoServiceImpl(shotstack, renders, recipes, mediaRepository,
                    storage, mediaHelper, transactionManager, mock(VideoThemeSelector.class), mock(VideoSlides.class));
            context.registerBean(RecipeVideoService.class, () -> service);
            context.refresh();
            var callback = new ShotstackWebhookRequest();
            callback.setId("render-1");
            callback.setType("edit");
            callback.setAction("render");
            callback.setStatus("done");
            callback.setUrl("https://untrusted.test/video.mp4");
            shotstack.handleWebhook(callback);
            assertEquals(completed ? "DONE" : "FAILED", render.getStatus());
            assertNotNull(render.getCompletedAt());
            verify(renders).save(render);
            verify(shotstack, never()).downloadOutput("https://untrusted.test/video.mp4");
            if (completed) {
                verify(storage).upload(eq(R2BucketType.PUBLIC), anyString(), eq(videoBytes), eq("video/mp4"));
                assertEquals(STORED_URL, recipe.getMedia().getUrl());
                assertEquals(STORED_URL, render.getVideoUrl());
                verify(recipes).save(recipe);
            } else {
                assertEquals(error, render.getErrorMessage());
                verifyNoInteractions(recipes, mediaRepository, storage);
            }
        }
        server.verify();
    }
}
