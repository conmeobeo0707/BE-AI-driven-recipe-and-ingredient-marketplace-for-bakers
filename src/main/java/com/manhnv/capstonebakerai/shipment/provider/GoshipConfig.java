package com.manhnv.capstonebakerai.shipment.provider;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(GoshipProperties.class)
public class GoshipConfig {

    @Bean
    RestClient goshipRestClient(GoshipProperties props) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
                factory.setReadTimeout(Duration.ofSeconds(15));

                return RestClient.builder()
                        .baseUrl(props.baseUrl())
                        .requestFactory(factory)
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.token())
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build();
    }

}
