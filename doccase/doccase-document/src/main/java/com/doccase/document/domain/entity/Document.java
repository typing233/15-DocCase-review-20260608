package com.doccase.document.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.doccase.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "dc_document", autoResultMap = true)
public class Document extends BaseEntity {

    private Long userId;
    private String title;
    private String description;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String fileHash;
    private String storageType;
    private String storagePath;
    private String thumbnailPath;
    private Integer currentVersion;
    private Integer status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> tagIds;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

    private Integer ocrStatus;
    private String ocrText;
}
