package com.manhnv.capstonebakerai.media.service.impl;

import tools.jackson.databind.JsonNode;
import com.manhnv.capstonebakerai.media.dto.request.ShotstackWebhookRequest;
import com.manhnv.capstonebakerai.media.event.ShotstackRenderVerifiedEvent;
import com.manhnv.capstonebakerai.media.service.ShotstackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShotstackServiceImpl implements ShotstackService {
    private final RestClient shotstackRestClient;
    private final ApplicationEventPublisher eventPublisher;
    private final RestClient downloadClient = RestClient.create();

    @Value("${app.shotstack.api-key:}")
    private String apiKey;
    @Value("${app.shotstack.callback-url}")
    private String callbackUrl;

    @Override
    public String createRender(Map<String, Object> edit) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Shotstack API key is not configured");
        }
        Map<String, Object> payload = new LinkedHashMap<>(edit);
        if (callbackUrl != null && !callbackUrl.isBlank()) payload.put("callback", callbackUrl);
        JsonNode queued;
        try {
            queued = shotstackRestClient.post().uri("/render")
                    .body(payload).retrieve().body(JsonNode.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Shotstack render request failed", e);
        }
        String renderId = queued == null ? null : queued.path("response").path("id").asText(null);
        if (renderId == null || renderId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Shotstack did not return a render ID");
        }
        return renderId;
    }

    @Override
    public byte[] downloadOutput(String url) {
        if (url == null || !url.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Shotstack output URL is invalid");
        }
        byte[] content;
        try {
            content = downloadClient.get().uri(URI.create(url)).retrieve().body(byte[].class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not download Shotstack output", e);
        }
        if (content == null || content.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Shotstack output is empty");
        }
        return content;
    }

    @Override
    public void handleWebhook(ShotstackWebhookRequest webhook) {
        if (webhook.getId() == null || webhook.getId().isBlank()
                || !"edit".equalsIgnoreCase(webhook.getType())
                || !"render".equalsIgnoreCase(webhook.getAction())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Shotstack render webhook");
        }
        // Shotstack webhooks are unsigned: verify with the provider before publishing the result.
        JsonNode authoritative;
        try {
            authoritative = shotstackRestClient.get().uri("/render/{id}", webhook.getId())
                    .retrieve().body(JsonNode.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not verify Shotstack webhook", e);
        }
        JsonNode details = authoritative == null ? null : authoritative.path("response");
        String status = details == null ? "" : details.path("status").asText("");
        if (status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Shotstack status response is incomplete");
        }
        // Synchronous delivery keeps callback acknowledgement after the recipe update succeeds.
        eventPublisher.publishEvent(new ShotstackRenderVerifiedEvent(webhook.getId(), status,
                details.hasNonNull("url") ? details.path("url").asText() : null,
                details.hasNonNull("error") ? details.path("error").asText() : null));
    }
}
