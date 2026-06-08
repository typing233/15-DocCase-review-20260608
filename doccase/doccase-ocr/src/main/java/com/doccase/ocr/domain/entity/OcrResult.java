package com.doccase.ocr.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "dc_ocr_result", autoResultMap = true)
public class OcrResult implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;

    private Long documentId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private String fullText;

    private Double confidence;

    private Integer pageCount;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> pageResults;

    private Long processingTimeMs;

    private String engine;

    private String language;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
