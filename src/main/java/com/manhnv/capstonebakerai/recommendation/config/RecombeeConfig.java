package com.manhnv.capstonebakerai.recommendation.config;

import com.recombee.api_client.RecombeeClient;
import com.recombee.api_client.util.Region;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.recombee.enabled", havingValue = "true")
public class RecombeeConfig {
    @Bean
    RecombeeClient recombeeClient(
            @Value("${RECOMBEE_DATABASE_ID:}") String databaseId,
            @Value("${RECOMBEE_PRIVATE_TOKEN:}") String privateToken,
            @Value("${RECOMBEE_REGION:AP_SE}") String region
    ) {
        if (databaseId.isBlank() || privateToken.isBlank()) {
            throw new IllegalStateException("Recombee is enabled but its database ID or private token is missing");
        }
        return new RecombeeClient(databaseId, privateToken)
                .setRegion(Region.valueOf(region));
    }
}
