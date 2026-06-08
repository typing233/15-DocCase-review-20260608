package com.doccase.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_audit_log")
public class AuditLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String detail;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
