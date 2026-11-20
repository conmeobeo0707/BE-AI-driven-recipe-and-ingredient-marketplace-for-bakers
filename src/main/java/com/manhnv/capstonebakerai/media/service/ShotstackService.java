package com.manhnv.capstonebakerai.media.service;

import com.manhnv.capstonebakerai.media.dto.request.ShotstackWebhookRequest;

import java.util.Map;

public interface ShotstackService {
    String createRender(Map<String, Object> edit);

    void handleWebhook(ShotstackWebhookRequest webhook);

    byte[] downloadOutput(String url);

    /**
     * Actively checks a render's status with Shotstack and publishes the same
     * {@code ShotstackRenderVerifiedEvent} the webhook would — a fallback for when Shotstack's
     * callback never arrives (e.g. a dev-only ngrok tunnel that isn't running), driven by
     * {@code RecipeVideoRenderPoller} instead of an inbound webhook call.
     */
    void pollAndPublish(String renderId);
}
