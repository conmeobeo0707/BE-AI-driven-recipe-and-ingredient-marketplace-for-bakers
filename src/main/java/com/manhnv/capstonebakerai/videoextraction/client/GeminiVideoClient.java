package com.manhnv.capstonebakerai.videoextraction.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.videoextraction.config.RecipeVideoExtractionProperties;
import com.manhnv.capstonebakerai.videoextraction.exception.RecipeVideoExtractionErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiVideoClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);

    private final RecipeVideoExtractionProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public RawExtractedRecipe extractRecipe(byte[] video, String mimeType, String prompt) {
        GeminiFile file = null;
        try {
            file = upload(video, mimeType);
            file = waitUntilActive(file);
            String json = generateWithRetry(file, prompt);
            return objectMapper.readValue(json, RawExtractedRecipe.class);
        } catch (RetryableStatusException exception) {
            throw new AppException(exception.status == 429
                    ? RecipeVideoExtractionErrorCode.AI_QUOTA_EXCEEDED
                    : RecipeVideoExtractionErrorCode.AI_UNAVAILABLE);
        } catch (HttpTimeoutException exception) {
            log.warn("Gemini recipe extraction timed out");
            throw new AppException(RecipeVideoExtractionErrorCode.AI_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException(RecipeVideoExtractionErrorCode.AI_UNAVAILABLE);
        } catch (IOException exception) {
            log.warn("Gemini recipe extraction connection failed: {}", exception.getMessage());
            throw new AppException(RecipeVideoExtractionErrorCode.AI_UNAVAILABLE);
        } catch (JacksonException exception) {
            log.warn("Gemini recipe extraction returned invalid JSON: {}", exception.getOriginalMessage());
            throw new AppException(RecipeVideoExtractionErrorCode.AI_INVALID_RESPONSE);
        } finally {
            if (file != null) {
                deleteQuietly(file.name());
            }
        }
    }

    private GeminiFile upload(byte[] video, String mimeType) throws IOException, InterruptedException {
        HttpRequest start = baseRequest(URI.create(BASE_URL + "/upload/v1beta/files"), Duration.ofSeconds(30))
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", String.valueOf(video.length))
                .header("X-Goog-Upload-Header-Content-Type", mimeType)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(Map.of("file", Map.of("display_name", "recipe-video")))))
                .build();
        HttpResponse<String> startResponse = httpClient.send(start, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(startResponse, "start upload");
        String uploadUrl = startResponse.headers().firstValue("x-goog-upload-url")
                .orElseThrow(() -> {
                    log.warn("Gemini upload start returned no upload URL");
                    return new AppException(RecipeVideoExtractionErrorCode.AI_UNAVAILABLE);
                });

        HttpRequest upload = baseRequest(URI.create(uploadUrl), Duration.ofMinutes(3))
                .header("X-Goog-Upload-Offset", "0")
                .header("X-Goog-Upload-Command", "upload, finalize")
                .POST(HttpRequest.BodyPublishers.ofByteArray(video))
                .build();
        HttpResponse<String> uploadResponse = httpClient.send(upload, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(uploadResponse, "upload");
        FileEnvelope envelope = objectMapper.readValue(uploadResponse.body(), FileEnvelope.class);
        if (envelope == null || envelope.file() == null || envelope.file().name() == null) {
            log.warn("Gemini upload returned no file");
            throw new AppException(RecipeVideoExtractionErrorCode.AI_UNAVAILABLE);
        }
        return envelope.file();
    }

    private GeminiFile waitUntilActive(GeminiFile file) throws IOException, InterruptedException {
        Instant deadline = Instant.now().plus(properties.getProcessingTimeout());
        GeminiFile current = file;
        while (!"ACTIVE".equals(current.state())) {
            if ("FAILED".equals(current.state())) {
                log.warn("Gemini could not process the uploaded video {}", current.name());
                throw new AppException(RecipeVideoExtractionErrorCode.VIDEO_NOT_READABLE);
            }
            if (Instant.now().isAfter(deadline)) {
                throw new AppException(RecipeVideoExtractionErrorCode.AI_TIMEOUT);
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
            HttpRequest get = baseRequest(URI.create(BASE_URL + "/v1beta/" + current.name()), Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "get file");
            current = objectMapper.readValue(response.body(), GeminiFile.class);
        }
        return current;
    }

    private String generateWithRetry(GeminiFile file, String prompt) throws IOException, InterruptedException {
        List<String> models = new ArrayList<>();
        models.add(properties.getModel());
        if (properties.getFallbackModels() != null) {
            properties.getFallbackModels().stream()
                    .filter(model -> model != null && !model.isBlank() && !models.contains(model.strip()))
                    .map(String::strip)
                    .forEach(models::add);
        }
        int attemptsPerModel = Math.max(1, properties.getMaxAttemptsPerModel());

        RetryableStatusException last = null;
        for (String model : models) {
            for (int attempt = 1; attempt <= attemptsPerModel; attempt++) {
                try {
                    return generate(file, prompt, model);
                } catch (RetryableStatusException exception) {
                    last = exception;
                    log.warn("Gemini model {} attempt {}/{} failed with HTTP {}",
                            model, attempt, attemptsPerModel, exception.status);
                    boolean moreAttempts = attempt < attemptsPerModel;
                    if (moreAttempts && exception.status != 429 && exception.status != 404) {
                        Thread.sleep(properties.getRetryBackoff().multipliedBy(attempt).toMillis());
                    } else {
                        break;
                    }
                }
            }
        }
        throw last;
    }

    private String generate(GeminiFile file, String prompt, String model) throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(
                                Map.of("file_data", Map.of(
                                        "mime_type", file.mimeType(),
                                        "file_uri", file.uri())),
                                Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", RecipeExtractionPrompt.responseSchema(),
                        "mediaResolution", "MEDIA_RESOLUTION_LOW",
                        "maxOutputTokens", 32768));

        HttpRequest request = baseRequest(
                URI.create(BASE_URL + "/v1beta/models/" + model + ":generateContent"),
                properties.getGenerateTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            log.warn("Gemini model {} is not available: {}", model, truncate(response.body()));
            throw new RetryableStatusException(404);
        }
        ensureSuccess(response, "generateContent");

        GenerateResponse body = objectMapper.readValue(response.body(), GenerateResponse.class);
        if (body.promptFeedback() != null && body.promptFeedback().blockReason() != null) {
            log.warn("Gemini blocked the video: {}", body.promptFeedback().blockReason());
            throw new AppException(RecipeVideoExtractionErrorCode.AI_REFUSED);
        }
        if (body.candidates() == null || body.candidates().isEmpty()) {
            throw new AppException(RecipeVideoExtractionErrorCode.AI_INVALID_RESPONSE);
        }
        Candidate candidate = body.candidates().getFirst();
        String finishReason = candidate.finishReason();
        if (finishReason != null && !"STOP".equals(finishReason)) {
            log.warn("Gemini recipe extraction finished with {}", finishReason);
            if ("MAX_TOKENS".equals(finishReason)) {
                throw new AppException(RecipeVideoExtractionErrorCode.AI_INVALID_RESPONSE);
            }
            throw new AppException(RecipeVideoExtractionErrorCode.AI_REFUSED);
        }
        String text = candidate.content() == null || candidate.content().parts() == null ? ""
                : candidate.content().parts().stream()
                .filter(part -> !Boolean.TRUE.equals(part.thought()))
                .map(Part::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
        if (text.isBlank()) {
            throw new AppException(RecipeVideoExtractionErrorCode.AI_INVALID_RESPONSE);
        }
        return text;
    }

    private void deleteQuietly(String fileName) {
        try {
            HttpRequest delete = baseRequest(URI.create(BASE_URL + "/v1beta/" + fileName), Duration.ofSeconds(15))
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(delete, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("Could not delete Gemini file {}: HTTP {}", fileName, response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException exception) {
            log.warn("Could not delete Gemini file {}: {}", fileName, exception.getMessage());
        }
    }

    private HttpRequest.Builder baseRequest(URI uri, Duration timeout) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(timeout)
                .header("x-goog-api-key", properties.getApiKey());
    }

    private void ensureSuccess(HttpResponse<String> response, String operation) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        log.warn("Gemini {} failed: HTTP {}, body={}", operation, status, truncate(response.body()));
        if (status == 429 || status >= 500) {
            throw new RetryableStatusException(status);
        }
        throw new AppException(RecipeVideoExtractionErrorCode.AI_UNAVAILABLE);
    }

    static final class RetryableStatusException extends RuntimeException {
        final int status;

        RetryableStatusException(int status) {
            super("Gemini returned HTTP " + status, null, false, false);
            this.status = status;
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= 500 ? body : body.substring(0, 500) + "...";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FileEnvelope(GeminiFile file) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiFile(String name, String uri, String mimeType, String state) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates, PromptFeedback promptFeedback) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text, Boolean thought) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PromptFeedback(String blockReason) {
    }
}
