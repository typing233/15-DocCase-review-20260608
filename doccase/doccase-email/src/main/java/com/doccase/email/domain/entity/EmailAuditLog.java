package com.doccase.email.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_email_audit_log")
public class EmailAuditLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private String action;

    private String messageId;

    private String attachmentName;

    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
