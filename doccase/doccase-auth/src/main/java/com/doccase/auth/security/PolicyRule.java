package com.doccase.auth.security;

import com.doccase.auth.domain.entity.AbacPolicy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@Data
public class PolicyRule {

    private String effect;
    private Map<String, Object> subjectCondition;
    private Map<String, Object> resourceCondition;
    private Map<String, Object> actionCondition;
    private Map<String, Object> environmentCondition;
    private int priority;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static PolicyRule fromEntity(AbacPolicy policy) {
        PolicyRule rule = new PolicyRule();
        rule.setEffect(policy.getEffect());
        rule.setPriority(policy.getPriority());
        try {
            rule.setSubjectCondition(objectMapper.readValue(policy.getSubjectCondition(), new TypeReference<>() {}));
            rule.setResourceCondition(objectMapper.readValue(policy.getResourceCondition(), new TypeReference<>() {}));
            rule.setActionCondition(objectMapper.readValue(policy.getActionCondition(), new TypeReference<>() {}));
            if (policy.getEnvironmentCondition() != null) {
                rule.setEnvironmentCondition(objectMapper.readValue(policy.getEnvironmentCondition(), new TypeReference<>() {}));
            }
        } catch (Exception e) {
            log.error("Failed to parse ABAC policy {}", policy.getId(), e);
        }
        return rule;
    }

    public boolean matches(Map<String, Object> subject, String action, Map<String, Object> resource, Map<String, Object> environment) {
        return matchesSubject(subject) && matchesAction(action) && matchesResource(resource) && matchesEnvironment(environment);
    }

    @SuppressWarnings("unchecked")
    private boolean matchesAction(String action) {
        if (actionCondition == null) return true;
        Object actions = actionCondition.get("actions");
        if (actions instanceof List<?> list) {
            return list.contains(action);
        }
        return true;
    }

    private boolean matchesSubject(Map<String, Object> subject) {
        if (subjectCondition == null) return true;
        for (Map.Entry<String, Object> entry : subjectCondition.entrySet()) {
            Object actual = subject.get(entry.getKey());
            if (actual == null || !actual.toString().equals(entry.getValue().toString())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesResource(Map<String, Object> resource) {
        if (resourceCondition == null) return true;
        for (Map.Entry<String, Object> entry : resourceCondition.entrySet()) {
            if (entry.getKey().equals("owner") && "${subject.id}".equals(entry.getValue())) {
                continue; // dynamic reference handled elsewhere
            }
            Object actual = resource.get(entry.getKey());
            if (actual == null || !actual.toString().equals(entry.getValue().toString())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesEnvironment(Map<String, Object> environment) {
        if (environmentCondition == null) return true;
        // Simplified: in production, check time ranges, IP whitelists, etc.
        return true;
    }
}
