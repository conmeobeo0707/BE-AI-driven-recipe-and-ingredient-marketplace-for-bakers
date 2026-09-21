package com.manhnv.capstonebakerai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(R2Properties.class)
public class R2Config {

    @Bean
    S3Client r2Client(R2Properties properties) {
        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                .credentialsProvider(credentialsProvider(properties))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Bean
    S3Presigner r2Presigner(R2Properties properties) {
        return S3Presigner.builder()
                .endpointOverride(properties.endpoint())
                .credentialsProvider(credentialsProvider(properties))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private StaticCredentialsProvider credentialsProvider(R2Properties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        properties.accessKeyId(),
                        properties.secretAccessKey()
                )
        );
    }
}
