package com.doccase.tag.rule.controller;

import com.doccase.common.domain.PageResult;
import com.doccase.common.dto.RuleEvaluateEvent;
import com.doccase.common.response.ApiResponse;
import com.doccase.tag.rule.domain.entity.AutoTagRule;
import com.doccase.tag.rule.domain.vo.RuleCreateRequest;
import com.doccase.tag.rule.domain.vo.RuleDryRunResult;
import com.doccase.tag.rule.service.RuleEngineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rules")
@RequiredArgsConstructor
public class AutoTagRuleController {

    private final RuleEngineService ruleEngineService;

    @GetMapping
    public ApiResponse<PageResult<AutoTagRule>> listRules(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestParam(required = false) String triggerEvent,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(ruleEngineService.listRules(tenantId, triggerEvent, pageNum, pageSize));
    }

    @PostMapping
    public ApiResponse<AutoTagRule> createRule(
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid RuleCreateRequest request) {
        return ApiResponse.success(ruleEngineService.createRule(tenantId, userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AutoTagRule> updateRule(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "0") Long userId,
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId,
            @RequestBody @Valid RuleCreateRequest request) {
        return ApiResponse.success(ruleEngineService.updateRule(tenantId, id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable Long id) {
        ruleEngineService.deleteRule(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/enable")
    public ApiResponse<Void> enableRule(@PathVariable Long id, @RequestParam boolean enabled) {
        ruleEngineService.enableRule(id, enabled);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/rollout")
    public ApiResponse<Void> updateRollout(@PathVariable Long id, @RequestParam int percentage) {
        ruleEngineService.updateRollout(id, percentage);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/rollback/{version}")
    public ApiResponse<Void> rollbackRule(@PathVariable Long id, @PathVariable int version) {
        ruleEngineService.rollbackRule(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/dry-run")
    public ApiResponse<RuleDryRunResult> dryRun(
            @PathVariable Long id,
            @RequestBody RuleEvaluateEvent event) {
        return ApiResponse.success(ruleEngineService.dryRun(id, event));
    }

    @PostMapping("/reload")
    public ApiResponse<Void> reloadRules(
            @RequestHeader(value = "X-Tenant-Id", defaultValue = "default") String tenantId) {
        ruleEngineService.reloadRules(tenantId);
        return ApiResponse.success();
    }
}
