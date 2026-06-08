package com.doccase.tag.rule.domain.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RuleCreateRequest {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    private String description;

    private Integer priority;

    @NotNull(message = "条件树不能为空")
    private String conditionTree;

    @NotNull(message = "动作不能为空")
    private String actions;

    private String triggerEvent;

    private Integer rolloutPercentage;
}
