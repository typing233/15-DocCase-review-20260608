package com.doccase.search.service.impl;

import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.search.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${embedding.service-url:http://localhost:8090/embed}")
    private String embeddingServiceUrl;

    @Value("${embedding.dimensions:768}")
    private int dimensions;

    @Value("${embedding.batch-size:32}")
    private int batchSize;

    @Value("${embedding.cache-ttl-hours:24}")
    private int cacheTtlHours;

    public EmbeddingServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = new RestTemplate();
    }

    @Override
    @Retryable(retryFor = RestClientException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimensions];
        }

        String cacheKey = RedisKeyConstants.SEARCH_EMBEDDING_CACHE_PREFIX + hashText(text);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return deserializeVector(cached);
        }

        float[] embedding = callEmbeddingApi(text);

        redisTemplate.opsForValue().set(cacheKey, serializeVector(embedding), cacheTtlHours, TimeUnit.HOURS);

        return embedding;
    }

    @Override
    @Retryable(retryFor = RestClientException.class, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public float[][] generateBatchEmbeddings(String[] texts) {
        if (texts == null || texts.length == 0) {
            return new float[0][];
        }

        float[][] results = new float[texts.length][];
        List<Integer> uncachedIndices = new ArrayList<>();
        List<String> uncachedTexts = new ArrayList<>();

        for (int i = 0; i < texts.length; i++) {
            if (texts[i] == null || texts[i].isBlank()) {
                results[i] = new float[dimensions];
                continue;
            }
            String cacheKey = RedisKeyConstants.SEARCH_EMBEDDING_CACHE_PREFIX + hashText(texts[i]);
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                results[i] = deserializeVector(cached);
            } else {
                uncachedIndices.add(i);
                uncachedTexts.add(texts[i]);
            }
        }

        if (!uncachedTexts.isEmpty()) {
            for (int batch = 0; batch < uncachedTexts.size(); batch += batchSize) {
                int end = Math.min(batch + batchSize, uncachedTexts.size());
                List<String> batchTexts = uncachedTexts.subList(batch, end);

                float[][] batchResults = callBatchEmbeddingApi(batchTexts);

                for (int j = 0; j < batchResults.length; j++) {
                    int originalIdx = uncachedIndices.get(batch + j);
                    results[originalIdx] = batchResults[j];

                    String cacheKey = RedisKeyConstants.SEARCH_EMBEDDING_CACHE_PREFIX + hashText(texts[originalIdx]);
                    redisTemplate.opsForValue().set(cacheKey, serializeVector(batchResults[j]),
                            cacheTtlHours, TimeUnit.HOURS);
                }
            }
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private float[] callEmbeddingApi(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of("text", text);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(embeddingServiceUrl, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("embedding")) {
                List<Number> embedding = (List<Number>) response.getBody().get("embedding");
                float[] result = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    result[i] = embedding.get(i).floatValue();
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("Embedding service call failed, returning zero vector: {}", e.getMessage());
        }
        return new float[dimensions];
    }

    @SuppressWarnings("unchecked")
    private float[][] callBatchEmbeddingApi(List<String> texts) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of("texts", texts);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    embeddingServiceUrl + "/batch", request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("embeddings")) {
                List<List<Number>> embeddings = (List<List<Number>>) response.getBody().get("embeddings");
                float[][] results = new float[embeddings.size()][];
                for (int i = 0; i < embeddings.size(); i++) {
                    List<Number> emb = embeddings.get(i);
                    results[i] = new float[emb.size()];
                    for (int j = 0; j < emb.size(); j++) {
                        results[i][j] = emb.get(j).floatValue();
                    }
                }
                return results;
            }
        } catch (Exception e) {
            log.warn("Batch embedding service call failed: {}", e.getMessage());
        }
        float[][] fallback = new float[texts.size()][];
        for (int i = 0; i < texts.size(); i++) {
            fallback[i] = new float[dimensions];
        }
        return fallback;
    }

    private String hashText(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }

    private String serializeVector(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    private float[] deserializeVector(String serialized) {
        String[] parts = serialized.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i]);
        }
        return vector;
    }
}
