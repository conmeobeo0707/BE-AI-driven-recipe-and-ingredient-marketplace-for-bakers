package com.manhnv.capstonebakerai.videoextraction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.recipe-video-extraction")
public class RecipeVideoExtractionProperties {

    private boolean enabled = false;

    private String apiKey = "";

    private String model = "gemini-flash-latest";

    private List<String> fallbackModels = List.of("gemini-3.5-flash", "gemini-flash-lite-latest", "gemini-3.8-flash");

    private int maxAttemptsPerModel = 2;

    private Duration retryBackoff = Duration.ofSeconds(3);

    private Duration processingTimeout = Duration.ofMinutes(3);

    private Duration generateTimeout = Duration.ofMinutes(4);

    private long maxVideoBytes = 100L * 1024 * 1024;
}
