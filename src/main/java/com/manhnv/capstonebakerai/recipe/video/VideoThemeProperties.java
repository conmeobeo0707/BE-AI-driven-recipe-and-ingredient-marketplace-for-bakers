package com.manhnv.capstonebakerai.recipe.video;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.video-theme")
public class VideoThemeProperties {

    private boolean llmEnabled = true;

    private String apiKey = "";

    private String model = "qwen/qwen3.8-27b";
}
