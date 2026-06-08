package com.doccase.email.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_email_archive_record")
public class EmailArchiveRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private String tenantId;

    private String messageId;

    private Long messageUid;

    private String fromAddress;

    private String subject;

    private LocalDateTime receivedAt;

    private String attachmentFileName;

    private String attachmentHash;

    private Long attachmentSize;

    private String attachmentMimeType;

    private Boolean isEncrypted;

    private Boolean decryptionAttempted;

    private Long documentId;

    private Integer status;

    private String skipReason;

    private String errorMessage;

    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
