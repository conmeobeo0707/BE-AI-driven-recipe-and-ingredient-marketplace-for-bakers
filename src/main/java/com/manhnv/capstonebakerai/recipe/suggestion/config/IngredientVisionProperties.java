package com.manhnv.capstonebakerai.recipe.suggestion.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ingredient-vision")
public class IngredientVisionProperties {

    private boolean enabled = false;

    private String apiKey = "";

    private String model = "qwen/qwen3.8-27b";
}
