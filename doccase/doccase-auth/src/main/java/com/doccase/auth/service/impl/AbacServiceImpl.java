package com.doccase.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.auth.domain.entity.AbacPolicy;
import com.doccase.auth.feign.UserFeignClient;
import com.doccase.auth.mapper.AbacPolicyMapper;
import com.doccase.auth.security.PolicyRule;
import com.doccase.auth.service.AbacService;
import com.doccase.common.dto.UserDTO;
import com.doccase.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AbacServiceImpl implements AbacService {

    private final AbacPolicyMapper abacPolicyMapper;
    private final UserFeignClient userFeignClient;
    private final StringRedisTemplate redisTemplate;

    private static final String POLICY_CACHE_KEY = "abac:policies:all";

    @Override
    public boolean evaluate(Long userId, String action, String resourceType, Long resourceId, Map<String, Object> environment) {
        Map<String, Object> subject = buildSubjectContext(userId);
        if (subject == null) {
            return false;
        }

        Map<String, Object> resource = new HashMap<>();
        resource.put("type", resourceType);
        resource.put("id", resourceId);

        List<PolicyRule> rules = loadPolicies();

        rules.sort(Comparator.comparingInt(PolicyRule::getPriority).reversed());

        for (PolicyRule rule : rules) {
            if (rule.matches(subject, action, resource, environment)) {
                boolean allowed = "ALLOW".equalsIgnoreCase(rule.getEffect());
                log.debug("ABAC decision for userId={}, action={}, resource={}:{} => {}",
                        userId, action, resourceType, resourceId, allowed ? "ALLOW" : "DENY");
                return allowed;
            }
        }

        return true;
    }

    @Override
    public AbacPolicy createPolicy(AbacPolicy policy) {
        policy.setIsEnabled(1);
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        abacPolicyMapper.insert(policy);
        redisTemplate.delete(POLICY_CACHE_KEY);
        return policy;
    }

    @Override
    public void deletePolicy(Long policyId) {
        abacPolicyMapper.deleteById(policyId);
        redisTemplate.delete(POLICY_CACHE_KEY);
    }

    private Map<String, Object> buildSubjectContext(Long userId) {
        ApiResponse<UserDTO> resp = userFeignClient.getUserDTO(userId);
        if (resp == null || resp.getData() == null) {
            return null;
        }
        UserDTO user = resp.getData();
        Map<String, Object> subject = new HashMap<>();
        subject.put("id", user.getId().toString());
        subject.put("username", user.getUsername());
        subject.put("roles", user.getRoles() != null ? String.join(",", user.getRoles()) : "");
        subject.put("status", user.getStatus() != null ? user.getStatus().toString() : "1");
        return subject;
    }

    private List<PolicyRule> loadPolicies() {
        LambdaQueryWrapper<AbacPolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AbacPolicy::getIsEnabled, 1);
        List<AbacPolicy> policies = abacPolicyMapper.selectList(wrapper);
        return policies.stream()
                .map(PolicyRule::fromEntity)
                .collect(Collectors.toList());
    }
}
