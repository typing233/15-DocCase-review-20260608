package com.doccase.tag.rule.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_auto_tag_execution_log")
public class AutoTagExecutionLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private Integer ruleVersion;

    private Long documentId;

    private String triggerEvent;

    private Boolean matched;

    private String actionsExecuted;

    private String errorMessage;

    private Integer executionTimeMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
