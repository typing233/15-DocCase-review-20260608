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

    private Long userId;

    private String engine;

    private String sourcePath;

    private String fileType;

    private String language;

    /**
     * Status: 0=pending, 1=preprocessing, 2=recognizing, 3=completed, 4=failed
     */
    private Integer status;

    private Integer retryCount;

    private Integer maxRetries;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
