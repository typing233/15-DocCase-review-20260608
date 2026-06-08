package com.doccase.email.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_email_account")
public class EmailAccount implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String tenantId;

    private String emailAddress;

    private String imapHost;

    private Integer imapPort;

    private Boolean useSsl;

    private String username;

    private String passwordEncrypted;

    private String folderFilter;

    private String attachmentFilter;

    private Integer pollIntervalMinutes;

    private Long lastPollUid;

    private LocalDateTime lastPollAt;

    private Boolean isEnabled;

    private Integer status;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime deletedAt;
}
