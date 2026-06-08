package com.doccase.tag.rule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doccase.common.constant.MqConstants;
import com.doccase.common.constant.RedisKeyConstants;
import com.doccase.common.domain.PageResult;
import com.doccase.common.dto.RuleEvaluateEvent;
import com.doccase.common.exception.BizException;
import com.doccase.tag.rule.domain.entity.AutoTagExecutionLog;
import com.doccase.tag.rule.domain.entity.AutoTagRule;
import com.doccase.tag.rule.domain.entity.AutoTagRuleVersion;
import com.doccase.tag.rule.domain.model.ConditionNode;
import com.doccase.tag.rule.domain.model.RuleAction;
import com.doccase.tag.rule.domain.vo.RuleCreateRequest;
import com.doccase.tag.rule.domain.vo.RuleDryRunResult;
import com.doccase.tag.rule.evaluator.ConditionEvaluator;
import com.doccase.tag.rule.mapper.AutoTagExecutionLogMapper;
import com.doccase.tag.rule.mapper.AutoTagRuleMapper;
import com.doccase.tag.rule.mapper.AutoTagRuleVersionMapper;
import com.doccase.tag.rule.service.RuleEngineService;
import com.doccase.tag.service.TagService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineServiceImpl implements RuleEngineService {

    private final AutoTagRuleMapper ruleMapper;
    private final AutoTagRuleVersionMapper versionMapper;
    private final AutoTagExecutionLogMapper executionLogMapper;
    private final ConditionEvaluator conditionEvaluator;
    private final TagService tagService;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    private final ConcurrentHashMap<String, List<AutoTagRule>> ruleCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadAllRules();
    }

    private void loadAllRules() {
        LambdaQueryWrapper<AutoTagRule> query = new LambdaQueryWrapper<>();
        query.eq(AutoTagRule::getIsEnabled, true)
                .orderByAsc(AutoTagRule::getPriority);
        List<AutoTagRule> allRules = ruleMapper.selectList(query);

        ruleCache.clear();
        for (AutoTagRule rule : allRules) {
            ruleCache.computeIfAbsent(rule.getTenantId() + ":" + rule.getTriggerEvent(),
                    k -> new ArrayList<>()).add(rule);
        }
        log.info("Loaded {} active rules into cache", allRules.size());
    }

    @Override
    @Transactional
    public AutoTagRule createRule(String tenantId, Long userId, RuleCreateRequest request) {
        // Validate condition tree
        ConditionNode condNode = parseConditionTree(request.getConditionTree());
        if (!conditionEvaluator.validateConditionTree(condNode)) {
            throw new BizException("Invalid condition tree");
        }

        AutoTagRule rule = new AutoTagRule();
        rule.setTenantId(tenantId);
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setIsEnabled(true);
        rule.setRolloutPercentage(request.getRolloutPercentage() != null ? request.getRolloutPercentage() : 100);
        rule.setVersion(1);
        rule.setConditionTree(request.getConditionTree());
        rule.setActions(request.getActions());
        rule.setTriggerEvent(request.getTriggerEvent() != null ? request.getTriggerEvent() : "DOCUMENT_CREATED");
        rule.setExecutionCount(0L);
        rule.setErrorCount(0L);
        rule.setCreatedBy(userId);
        rule.setCreatedAt(LocalDateTime.now());
        rule.setUpdatedAt(LocalDateTime.now());
        rule.setIsDeleted(0);

        ruleMapper.insert(rule);

        // Save initial version
        saveVersion(rule, userId);

        reloadRules(tenantId);
        return rule;
    }

    @Override
    @Transactional
    public AutoTagRule updateRule(String tenantId, Long ruleId, Long userId, RuleCreateRequest request) {
        AutoTagRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) throw new BizException("Rule not found");

        ConditionNode condNode = parseConditionTree(request.getConditionTree());
        if (!conditionEvaluator.validateConditionTree(condNode)) {
            throw new BizException("Invalid condition tree");
        }

        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        rule.setConditionTree(request.getConditionTree());
        rule.setActions(request.getActions());
        if (request.getTriggerEvent() != null) rule.setTriggerEvent(request.getTriggerEvent());
        if (request.getRolloutPercentage() != null) rule.setRolloutPercentage(request.getRolloutPercentage());
        rule.setVersion(rule.getVersion() + 1);
        rule.setUpdatedAt(LocalDateTime.now());

        ruleMapper.updateById(rule);
        saveVersion(rule, userId);

        reloadRules(tenantId);
        return rule;
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId) {
        AutoTagRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) return;

        rule.setIsDeleted(1);
        rule.setDeletedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);

        reloadRules(rule.getTenantId());
    }

    @Override
    public void enableRule(Long ruleId, boolean enabled) {
        AutoTagRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) throw new BizException("Rule not found");

        rule.setIsEnabled(enabled);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);

        reloadRules(rule.getTenantId());
    }

    @Override
    public void updateRollout(Long ruleId, int percentage) {
        if (percentage < 0 || percentage > 100) throw new BizException("Rollout percentage must be 0-100");

        AutoTagRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) throw new BizException("Rule not found");

        rule.setRolloutPercentage(percentage);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);

        reloadRules(rule.getTenantId());
    }

    @Override
    @Transactional
    public void rollbackRule(Long ruleId, int targetVersion) {
        AutoTagRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) throw new BizException("Rule not found");

        LambdaQueryWrapper<AutoTagRuleVersion> vQuery = new LambdaQueryWrapper<>();
        vQuery.eq(AutoTagRuleVersion::getRuleId, ruleId)
                .eq(AutoTagRuleVersion::getVersion, targetVersion);
        AutoTagRuleVersion version = versionMapper.selectOne(vQuery);
        if (version == null) throw new BizException("Version not found");

        rule.setConditionTree(version.getConditionTree());
        rule.setActions(version.getActions());
        rule.setRolloutPercentage(version.getRolloutPercentage());
        rule.setVersion(rule.getVersion() + 1);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(rule);

        saveVersion(rule, rule.getCreatedBy());
        reloadRules(rule.getTenantId());
    }

    @Override
    public void evaluateAndExecute(RuleEvaluateEvent event) {
        String cacheKey = event.getTenantId() + ":" + event.getTriggerEvent();
        List<AutoTagRule> rules = ruleCache.getOrDefault(cacheKey, Collections.emptyList());

        for (AutoTagRule rule : rules) {
            long startTime = System.currentTimeMillis();
            try {
                // Grayscale check
                if (!shouldExecute(rule, event.getDocumentId())) {
                    continue;
                }

                ConditionNode condNode = parseConditionTree(rule.getConditionTree());
                boolean matched = conditionEvaluator.evaluate(condNode, event);

                if (matched) {
                    List<RuleAction> actions = parseActions(rule.getActions());
                    executeActions(event.getTenantId(), event.getDocumentId(), actions);

                    rule.setExecutionCount(rule.getExecutionCount() + 1);
                    rule.setLastExecutedAt(LocalDateTime.now());
                    ruleMapper.updateById(rule);
                }

                logExecution(rule, event, matched, null, startTime);

            } catch (Exception e) {
                log.error("Rule {} execution failed for document {}", rule.getId(), event.getDocumentId(), e);
                rule.setErrorCount(rule.getErrorCount() + 1);
                ruleMapper.updateById(rule);
                logExecution(rule, event, false, e.getMessage(), startTime);
            }
        }
    }

    @Override
    public RuleDryRunResult dryRun(Long ruleId, RuleEvaluateEvent event) {
        AutoTagRule rule = ruleMapper.selectById(ruleId);
        if (rule == null) throw new BizException("Rule not found");

        ConditionNode condNode = parseConditionTree(rule.getConditionTree());
        boolean matched = conditionEvaluator.evaluate(condNode, event);

        List<String> actionsToExecute = new ArrayList<>();
        if (matched) {
            List<RuleAction> actions = parseActions(rule.getActions());
            actionsToExecute = actions.stream()
                    .map(a -> a.getType() + " -> tagId:" + a.getTagId())
                    .collect(Collectors.toList());
        }

        RuleDryRunResult.MatchDetail detail = RuleDryRunResult.MatchDetail.builder()
                .documentId(event.getDocumentId())
                .documentTitle(event.getTitle())
                .matched(matched)
                .actionsToExecute(actionsToExecute)
                .build();

        return RuleDryRunResult.builder()
                .ruleId(ruleId)
                .ruleName(rule.getName())
                .totalDocuments(1)
                .matchedDocuments(matched ? 1 : 0)
                .matches(List.of(detail))
                .build();
    }

    @Override
    public PageResult<AutoTagRule> listRules(String tenantId, String triggerEvent, int pageNum, int pageSize) {
        LambdaQueryWrapper<AutoTagRule> query = new LambdaQueryWrapper<>();
        query.eq(AutoTagRule::getTenantId, tenantId);
        if (triggerEvent != null) query.eq(AutoTagRule::getTriggerEvent, triggerEvent);
        query.orderByAsc(AutoTagRule::getPriority);

        Page<AutoTagRule> page = ruleMapper.selectPage(new Page<>(pageNum, pageSize), query);
        return PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    public void reloadRules(String tenantId) {
        LambdaQueryWrapper<AutoTagRule> query = new LambdaQueryWrapper<>();
        query.eq(AutoTagRule::getTenantId, tenantId)
                .eq(AutoTagRule::getIsEnabled, true)
                .orderByAsc(AutoTagRule::getPriority);
        List<AutoTagRule> tenantRules = ruleMapper.selectList(query);

        // Clear existing rules for this tenant
        ruleCache.entrySet().removeIf(e -> e.getKey().startsWith(tenantId + ":"));

        for (AutoTagRule rule : tenantRules) {
            ruleCache.computeIfAbsent(tenantId + ":" + rule.getTriggerEvent(),
                    k -> new ArrayList<>()).add(rule);
        }

        // Publish reload event for cluster-wide invalidation
        redisTemplate.convertAndSend(RedisKeyConstants.RULE_RELOAD_CHANNEL, tenantId);
        log.info("Reloaded {} rules for tenant {}", tenantRules.size(), tenantId);
    }

    private boolean shouldExecute(AutoTagRule rule, Long documentId) {
        if (rule.getRolloutPercentage() >= 100) return true;
        if (rule.getRolloutPercentage() <= 0) return false;
        return (Math.abs(documentId.hashCode()) % 100) < rule.getRolloutPercentage();
    }

    private void executeActions(String tenantId, Long documentId, List<RuleAction> actions) {
        for (RuleAction action : actions) {
            if ("ADD_TAG".equalsIgnoreCase(action.getType())) {
                tagService.addDocumentTag(tenantId, documentId, action.getTagId());
            } else if ("REMOVE_TAG".equalsIgnoreCase(action.getType())) {
                tagService.removeDocumentTag(tenantId, documentId, action.getTagId());
            }
        }
    }

    private void saveVersion(AutoTagRule rule, Long userId) {
        AutoTagRuleVersion version = new AutoTagRuleVersion();
        version.setRuleId(rule.getId());
        version.setVersion(rule.getVersion());
        version.setConditionTree(rule.getConditionTree());
        version.setActions(rule.getActions());
        version.setRolloutPercentage(rule.getRolloutPercentage());
        version.setCreatedBy(userId);
        version.setCreatedAt(LocalDateTime.now());
        versionMapper.insert(version);
    }

    private void logExecution(AutoTagRule rule, RuleEvaluateEvent event, boolean matched,
                              String error, long startTime) {
        AutoTagExecutionLog execLog = new AutoTagExecutionLog();
        execLog.setRuleId(rule.getId());
        execLog.setRuleVersion(rule.getVersion());
        execLog.setDocumentId(event.getDocumentId());
        execLog.setTriggerEvent(event.getTriggerEvent());
        execLog.setMatched(matched);
        execLog.setErrorMessage(error);
        execLog.setExecutionTimeMs((int) (System.currentTimeMillis() - startTime));
        execLog.setCreatedAt(LocalDateTime.now());
        if (matched) {
            execLog.setActionsExecuted(rule.getActions());
        }
        executionLogMapper.insert(execLog);
    }

    private ConditionNode parseConditionTree(String json) {
        try {
            return objectMapper.readValue(json, ConditionNode.class);
        } catch (Exception e) {
            throw new BizException("Invalid condition tree JSON: " + e.getMessage());
        }
    }

    private List<RuleAction> parseActions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<RuleAction>>() {});
        } catch (Exception e) {
            throw new BizException("Invalid actions JSON: " + e.getMessage());
        }
    }
}
