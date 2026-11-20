package com.manhnv.capstonebakerai.media.controller;

import com.manhnv.capstonebakerai.common.response.ApiResponse;
import com.manhnv.capstonebakerai.media.dto.request.ShotstackWebhookRequest;
import com.manhnv.capstonebakerai.media.service.ShotstackService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media/videos")
@RequiredArgsConstructor
public class ShotstackWebhookController {
    private final ShotstackService shotstackService;

    @Operation(summary = "Webhook Shotstack cập nhật trạng thái render", description = "Shotstack gửi callback khi render hoàn tất hoặc thất bại.")
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> shotstackWebhook(@RequestBody ShotstackWebhookRequest request) {
        shotstackService.handleWebhook(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
