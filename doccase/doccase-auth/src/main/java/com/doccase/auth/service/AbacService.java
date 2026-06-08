package com.doccase.auth.service;

import com.doccase.auth.domain.entity.AbacPolicy;

import java.util.Map;

public interface AbacService {

    boolean evaluate(Long userId, String action, String resourceType, Long resourceId, Map<String, Object> environment);

    AbacPolicy createPolicy(AbacPolicy policy);

    void deletePolicy(Long policyId);
}
