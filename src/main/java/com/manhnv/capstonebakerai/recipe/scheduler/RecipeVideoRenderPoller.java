package com.manhnv.capstonebakerai.recipe.scheduler;

import com.manhnv.capstonebakerai.media.service.ShotstackService;
import com.manhnv.capstonebakerai.recipe.entity.RecipeVideoRender;
import com.manhnv.capstonebakerai.recipe.repository.RecipeVideoRenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fallback for {@code RecipeVideoController.createRecipeVideo}: Shotstack's own callback
 * ({@code app.shotstack.callback-url}) is how a render normally finishes and gets its video
 * attached to the recipe, but that callback depends on the configured URL actually being
 * reachable (e.g. a dev-only ngrok tunnel that isn't running, or one that was restarted with a
 * new URL the config wasn't updated for). Without this, a render whose callback never arrives
 * stays PROCESSING forever — blocking any new attempt for that recipe (409) and never saving a
 * video URL. This periodically re-checks every still-PROCESSING render directly with Shotstack
 * and runs the same attach/fail logic the webhook would, so the render reliably finishes (gets
 * its URL saved onto the recipe) in the background even if the webhook never shows up.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeVideoRenderPoller {
    private static final String PROCESSING = "PROCESSING";

    private final RecipeVideoRenderRepository renderRepository;
    private final ShotstackService shotstackService;

    @Scheduled(fixedDelay = 20_000, initialDelay = 20_000)
    public void run() {
        List<RecipeVideoRender> processing = renderRepository.findAllByStatus(PROCESSING);
        for (RecipeVideoRender render : processing) {
            try {
                shotstackService.pollAndPublish(render.getRenderId());
            } catch (Exception e) {
                // Still PROCESSING on Shotstack's side, or a transient call failure — just retry
                // on the next tick instead of letting one render's error block the others.
                log.debug("Recipe video render {} chưa xong hoặc chưa kiểm tra được: {}", render.getRenderId(), e.getMessage());
            }
        }
    }
}
