package com.manhnv.capstonebakerai.recommendation.config;

import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Weights;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Settings under app.recommendation.* in application.properties.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.recommendation")
public class RecommendationProperties {

    // Feature weights used while no trained model is active.
    private DefaultWeights defaultWeights = new DefaultWeights();

    // Minimum data before training is allowed.
    private Training training = new Training();

    public Weights defaultWeights() {
        return new Weights(defaultWeights.content, defaultWeights.skill, defaultWeights.hobby,
                defaultWeights.collaborative, defaultWeights.quality);
    }

    @Getter
    @Setter
    public static class DefaultWeights {
        private double content = 0.35;
        private double skill = 0.20;
        private double hobby = 0.15;
        private double collaborative = 0.15;
        private double quality = 0.15;
    }

    @Getter
    @Setter
    public static class Training {
        // Customers with 3+ likes (their last like is the test case).
        private int minTestCustomers = 20;
        // Likes available to learn from.
        private int minPositiveSamples = 100;
    }
}
