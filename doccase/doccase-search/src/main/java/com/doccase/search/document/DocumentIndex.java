package com.doccase.search.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentIndex implements Serializable {

    private Long id;

    private String tenantId;

    private String title;

    private String description;

    private String fileName;

    private String fileType;

    private String mimeType;

    private Long userId;

    private List<Long> tagIds;

    private List<String> tagNames;

    private String ocrText;

    private Map<String, Object> metadata;

    private Integer status;

    private Long fileSize;

    private float[] contentVector;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
