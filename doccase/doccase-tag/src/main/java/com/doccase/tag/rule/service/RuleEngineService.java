package com.doccase.tag.rule.service;

import com.doccase.common.domain.PageResult;
import com.doccase.common.dto.RuleEvaluateEvent;
import com.doccase.tag.rule.domain.entity.AutoTagRule;
import com.doccase.tag.rule.domain.vo.RuleCreateRequest;
import com.doccase.tag.rule.domain.vo.RuleDryRunResult;

public interface RuleEngineService {

    AutoTagRule createRule(String tenantId, Long userId, RuleCreateRequest request);

    AutoTagRule updateRule(String tenantId, Long ruleId, Long userId, RuleCreateRequest request);

    void deleteRule(Long ruleId);

    void enableRule(Long ruleId, boolean enabled);

    void updateRollout(Long ruleId, int percentage);

    void rollbackRule(Long ruleId, int targetVersion);

    void evaluateAndExecute(RuleEvaluateEvent event);

    RuleDryRunResult dryRun(Long ruleId, RuleEvaluateEvent event);

    PageResult<AutoTagRule> listRules(String tenantId, String triggerEvent, int pageNum, int pageSize);

    void reloadRules(String tenantId);
}
