package com.doccase.tag.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_tag_operation_log")
public class TagOperationLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantId;

    private String operationType;

    private String sourceTagIds;

    private Long targetTagId;

    private String documentIds;

    private Long operatorId;

    private Integer status;

    private String conflictDetail;

    private String resultDetail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}
