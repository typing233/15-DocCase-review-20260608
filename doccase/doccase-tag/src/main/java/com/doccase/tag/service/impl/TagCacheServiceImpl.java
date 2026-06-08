package com.doccase.tag.service.impl;

import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.tag.domain.vo.TagTreeVO;
import com.doccase.tag.service.TagCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagCacheServiceImpl implements TagCacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final long CACHE_TTL_MINUTES = 30;

    @Override
    public List<TagTreeVO> getCachedTagTree(String tenantId) {
        String cacheKey = RedisKeyConstants.TAG_TREE_TENANT_PREFIX + tenantId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<TagTreeVO>>() {});
            } catch (Exception e) {
                log.warn("Failed to deserialize cached tag tree for tenant {}", tenantId, e);
                redisTemplate.delete(cacheKey);
            }
        }
        return null;
    }

    public void cacheTagTree(String tenantId, List<TagTreeVO> tree) {
        String cacheKey = RedisKeyConstants.TAG_TREE_TENANT_PREFIX + tenantId;
        try {
            String json = objectMapper.writeValueAsString(tree);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            String versionKey = RedisKeyConstants.TAG_TREE_VERSION_PREFIX + tenantId;
            redisTemplate.opsForValue().increment(versionKey);
        } catch (Exception e) {
            log.warn("Failed to cache tag tree for tenant {}", tenantId, e);
        }
    }

    @Override
    public void invalidateTagTree(String tenantId) {
        String cacheKey = RedisKeyConstants.TAG_TREE_TENANT_PREFIX + tenantId;
        redisTemplate.delete(cacheKey);

        String versionKey = RedisKeyConstants.TAG_TREE_VERSION_PREFIX + tenantId;
        redisTemplate.opsForValue().increment(versionKey);
        log.debug("Invalidated tag tree cache for tenant {}", tenantId);
    }

    @Override
    public void invalidateAllTagTrees() {
        var keys = redisTemplate.keys(RedisKeyConstants.TAG_TREE_TENANT_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public boolean tagExists(String tenantId, Long tagId) {
        RBloomFilter<Long> bloomFilter = getBloomFilter(tenantId);
        return bloomFilter.contains(tagId);
    }

    @Override
    public void addTagToBloom(String tenantId, Long tagId) {
        RBloomFilter<Long> bloomFilter = getBloomFilter(tenantId);
        bloomFilter.add(tagId);
    }

    private RBloomFilter<Long> getBloomFilter(String tenantId) {
        String filterName = RedisKeyConstants.TAG_BLOOM_PREFIX + tenantId;
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(filterName);
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(100000, 0.001);
        }
        return bloomFilter;
    }
}
