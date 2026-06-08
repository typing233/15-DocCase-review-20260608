package com.doccase.ocr.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("dc_ocr_task")
public class OcrTask implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long documentId;

    private String engine;

    private String language;

    /**
     * Status: 0=pending, 1=processing, 2=completed, 3=failed
     */
    private Integer status;

    private Integer retryCount;

    private Integer maxRetries;

    private String errorMessage;

    private Long processingTimeMs;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
