package com.doccase.tag.rule.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_auto_tag_rule_version")
public class AutoTagRuleVersion implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long ruleId;

    private Integer version;

    private String conditionTree;

    private String actions;

    private Integer rolloutPercentage;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
