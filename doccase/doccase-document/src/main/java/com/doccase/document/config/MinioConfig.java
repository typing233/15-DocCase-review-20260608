package com.doccase.document.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${doccase.storage.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${doccase.storage.minio.access-key:doccase}")
    private String accessKey;

    @Value("${doccase.storage.minio.secret-key:doccase123456}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
