package com.doccase.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_abac_policy")
public class AbacPolicy {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String name;
    private String description;
    private String effect;
    private String subjectCondition;
    private String resourceCondition;
    private String actionCondition;
    private String environmentCondition;
    private Integer priority;
    private Integer isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
