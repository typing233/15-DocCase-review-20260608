package com.doccase.tag.rule.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_auto_tag_rule")
public class AutoTagRule implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantId;

    private String name;

    private String description;

    private Integer priority;

    private Boolean isEnabled;

    private Integer rolloutPercentage;

    private Integer version;

    private String conditionTree;

    private String actions;

    private String triggerEvent;

    private Long executionCount;

    private Long errorCount;

    private LocalDateTime lastExecutedAt;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime deletedAt;
}
