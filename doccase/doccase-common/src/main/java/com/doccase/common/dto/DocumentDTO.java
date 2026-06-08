package com.doccase.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DocumentDTO implements Serializable {

    private Long id;
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
    private List<Long> tagIds;
    private Map<String, Object> metadata;
    private Integer ocrStatus;
    private String ocrText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
