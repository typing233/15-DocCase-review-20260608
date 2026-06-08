package com.doccase.document.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dc_document_version")
public class DocumentVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long documentId;
    private Integer versionNumber;
    private String fileHash;
    private String storagePath;
    private Long fileSize;
    private String changeNote;
    private Long createdBy;
    private LocalDateTime createdAt;
}
