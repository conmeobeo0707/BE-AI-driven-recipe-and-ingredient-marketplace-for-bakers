package com.manhnv.capstonebakerai.media.service;

import com.manhnv.capstonebakerai.media.dto.request.ShotstackWebhookRequest;

import java.util.Map;

public interface ShotstackService {
    String createRender(Map<String, Object> edit);

    void handleWebhook(ShotstackWebhookRequest webhook);

    byte[] downloadOutput(String url);
}
